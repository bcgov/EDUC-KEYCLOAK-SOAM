import {sleep, group, check} from "k6";
import * as http from "k6/http";
import {Rate} from "k6/metrics";

// export const options = {
//     stages: [
//         { duration: "30s", target: 10 },
//         { duration: "1m", target: 50 },
//         { duration: "30s", target: 100 },
//         { duration: "30s", target: 50 },
//         { duration: "30s", target: 10 },
//     ],
//     discardResponseBodies: true
// };
export let options = {
    httpDebug: 'full',
    discardResponseBodies: true
};

let config = JSON.parse(open(__ENV.CONFIG));

export let errorRate = new Rate("errors");

function checkStatus200(response) {
    let success = check(response, {"is status 200": (r) => r.status === 200});
    //if(!success)
    errorRate.add(!success);
}

export default function main() {
    let response;

    const vars = {};

    group(
        "SOAM Initial Auth Call",
        function () {
            response = http.get(
                config.soam.rootUrl + config.soam.authPath + "?response_type=code&client_id=" + config.soam.clientId + "&kc_idp_hint=keycloak_bcdevexchange_bcsc&redirect_uri=" + encodeURI(config.soam.redirectUrl) + "&scope=" + config.soam.scopes,
                {
                    responseType: 'text',
                    tags: {
                        name: 'SOAMAuthURL'
                    }
                }
            );
            checkStatus200(response);

            vars["validateCardResult"] = response
                .html()
                .find("input[name=validateCardResult]")
                .first()
                .attr("value");
        }
    );
    group(
        "IDP Pages",
        function () {
            response = http.get('https://idtest.gov.bc.ca/login/entry',
                {
                    tags: {
                        name: 'LoginEntryURL'
                    }
                });
            checkStatus200(response);

            response = http.get("https://idtest.gov.bc.ca/cardtap/resources/templates.html?v=R2.9.2",
                {
                    tags: {
                        name: 'CardtapURL'
                    }
                });
            checkStatus200(response);

            response = http.post(
                "https://idtest.gov.bc.ca/cardtap/v3/transactions/1577032a-02f7-44da-8d7a-963c41a7289b?clientId=" + config.idim.clientId,
                null,
                {
                    headers: {
                        "content-type": "application/json"
                    },
                    tags: {
                        name: 'ClientIDURL'
                    }
                }
            );
            checkStatus200(response);

            response = http.put(
                "https://idtest.gov.bc.ca/cardtap/v3/transactions/1577032a-02f7-44da-8d7a-963c41a7289b/device",
                '{"deviceType":"MOCKSKAP"}',
                {
                    headers: {
                        "content-type": "application/json"
                    },
                    tags: {
                        name: 'DeviceURL'
                    }
                }
            );
            checkStatus200(response);

            response = http.post(
                "https://idtest.gov.bc.ca/MockSKAP/authorize",
                null,
                {
                    tags: {
                        name: 'IDMAuthorizeURL'
                    }
                }
            );
            checkStatus200(response);

            response = http.get("https://idtest.gov.bc.ca/MockSKAP/Widget/mockConnect.html",
                {
                    tags: {
                        name: 'MockConnectURL'
                    }
                });
            checkStatus200(response);

            response = http.post(
                "https://idtest.gov.bc.ca/MockSKAP/lookup-mbun-by-csn?csn=" + config.user.name,
                null,
                {
                    headers: {
                        "content-type": "application/json"
                    },
                    tags: {
                        name: 'LookupURL'
                    }
                }
            );
            checkStatus200(response);

            response = http.post(
                "https://idtest.gov.bc.ca/MockSKAP/cardread",
                '{"userId": ' + config.user.id + '}',
                {
                    headers: {
                        "content-type": "application/json"
                    },
                    responseType: 'text',
                    tags: {
                        name: 'CardReadURL'
                    }
                }
            );
            checkStatus200(response);

            let jwe = response.body.match('JWE":"([^"]*)"')[1];

            response = http.put(
                "https://idtest.gov.bc.ca/cardtap/v3/transactions/1577032a-02f7-44da-8d7a-963c41a7289b/jwe",
                '{"audience":"mockskap.app","JWE":"' + jwe + '"}',
                {
                    headers: {
                        "content-type": "application/json"
                    },
                    responseType: 'text',
                    tags: {
                        name: 'JWEURL'
                    }
                }
            );
            checkStatus200(response);

            let validateCardResultFromJwe = response.body;

            response = http.post(
                "https://idtest.gov.bc.ca/login/identify",
                {
                    validateCardResult:
                    validateCardResultFromJwe,
                    csrftoken: `${vars["validateCardResult"]}`,
                },
                {
                    tags: {
                        name: 'IdentifyURL'
                    }
                }
            );
            checkStatus200(response);

            response = http.post(
                "https://idtest.gov.bc.ca/login/passcode/validate",
                {
                    csrftoken: `${vars["validateCardResult"]}`,
                    passcode: config.user.secret,
                },
                {
                    tags: {
                        name: 'ValidateURL'
                    }
                }
            );
            checkStatus200(response);

            response = http.get("https://idtest.gov.bc.ca/login/history",
                {
                    tags: {
                        name: 'HistoryURL'
                    }
                });
            checkStatus200(response);
        }
    );

    group("SOAM Return Code", function () {
            response = http.post(
                "https://idtest.gov.bc.ca/login/setConfirmation",
                `csrftoken=${vars["validateCardResult"]}`,
                {
                    headers: {
                        "content-type": "application/x-www-form-urlencoded",
                    },
                    redirects: 3,
                    tags: {
                        name: 'SetConfirmationURL'
                    }
                }
            );

            let statusCheck = check(response, {
                "is status 302": (r) => r.status === 302
            });
            errorRate.add(!statusCheck);
            let codeCheck = check(response, {
                "is code present": (r) => {
                    if (r.headers && r.headers.Location) {
                        return r.headers.Location.includes("code=");
                    } else {
                        return false;
                    }

                }
            });
            errorRate.add(!codeCheck);

        }
    );

    group("SOAM Get Token", function () {

            let code = null;
            if (response.headers && response.headers.Location) {
                code = response.headers.Location.match("code=(.*)")[1];
            }
            response = http.post(config.soam.rootUrl + config.soam.tokenPath,
                {
                    client_id: config.soam.clientId,
                    client_secret: config.soam.clientSecret,
                    grant_type: 'authorization_code',
                    code: code,
                    redirect_uri: config.soam.redirectUrl
                },
                {
                    headers: {
                        Accept: 'application/json',
                        'Cache-Control': 'no-cache',
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    responseType: 'text',
                    tags: {
                        name: 'GetTokenURL'
                    }
                });
            checkStatus200(response);
            let codeCheck = check(response, {
                "is token present": (r) => {
                    if (r.body) {
                        return r.body.includes("access_token\":");
                    } else {
                        return false;
                    }

                }
            });
            errorRate.add(!codeCheck);
        }
    );
    // Automatically added sleep
    sleep(1);
}
