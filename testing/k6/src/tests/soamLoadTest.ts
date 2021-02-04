import {sleep, group, check} from "k6";
import * as http from "k6/http";
import {Rate} from "k6/metrics";

export const options = {
    stages: [
        { duration: "30s", target: 5 },
        { duration: "1m", target: 10 },
        { duration: "30s", target: 0 }
    ],
    discardResponseBodies: true
};
// Use this for debugging test instead of the above option
// export let options = {
//     httpDebug: 'full',
//     discardResponseBodies: true
// };

let config = JSON.parse(open(__ENV.CONFIG));

export let errorRate = new Rate("errors");

function checkStatus(response, statusCode=200) {
    let success = check(response, {"is correct status": (r) => r.status === statusCode});
    errorRate.add(!success, { tag1: 'is correct status' });
}
//Used to get a different user for each test
let userIdx = 0;

function getUserId() {
    return config.user[userIdx % config.user.length].id;
}

function getUserName() {
    return config.user[userIdx % config.user.length].name;
}

function getUserSecret() {
    return config.user[userIdx % config.user.length].secret;
}

export default function main() {
    let response;
    userIdx++;
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
            checkStatus(response);

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
            checkStatus(response);

            response = http.get("https://idtest.gov.bc.ca/cardtap/resources/templates.html?v=R2.9.2",
                {
                    tags: {
                        name: 'CardtapURL'
                    }
                });
            checkStatus(response);

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
            checkStatus(response);

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
            checkStatus(response);

            response = http.post(
                "https://idtest.gov.bc.ca/MockSKAP/authorize",
                null,
                {
                    tags: {
                        name: 'IDMAuthorizeURL'
                    }
                }
            );
            checkStatus(response);

            response = http.get("https://idtest.gov.bc.ca/MockSKAP/Widget/mockConnect.html",
                {
                    tags: {
                        name: 'MockConnectURL'
                    }
                });
            checkStatus(response);

            response = http.post(
                "https://idtest.gov.bc.ca/MockSKAP/lookup-mbun-by-csn?csn=" + getUserName(),
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
            checkStatus(response);

            response = http.post(
                "https://idtest.gov.bc.ca/MockSKAP/cardread",
                '{"userId": ' + getUserId() + '}',
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
            checkStatus(response);

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
            checkStatus(response);

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
            checkStatus(response);

            response = http.post(
                "https://idtest.gov.bc.ca/login/passcode/validate",
                {
                    csrftoken: `${vars["validateCardResult"]}`,
                    passcode: getUserSecret(),
                },
                {
                    tags: {
                        name: 'ValidateURL'
                    }
                }
            );
            checkStatus(response);

            response = http.get("https://idtest.gov.bc.ca/login/history",
                {
                    tags: {
                        name: 'HistoryURL'
                    }
                });
            checkStatus(response);
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
                    redirects: 3, //Limits the number of redirects so we don't return to final callback url
                    tags: {
                        name: 'SetConfirmationURL'
                    }
                }
            );

            checkStatus(response, 302);
            let codeCheck = check(response, {
                "is code present": (r) => {
                    if (r.headers && r.headers.Location) {
                        return r.headers.Location.includes("code=");
                    } else {
                        return false;
                    }

                }
            });
            errorRate.add(!codeCheck, { tag1: 'is code present' });

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
            checkStatus(response);
            let codeCheck = check(response, {
                "is token present": (r) => {
                    if (r.body) {
                        return r.body.includes("access_token\":");
                    } else {
                        return false;
                    }

                }
            });
            errorRate.add(!codeCheck, { tag1: 'is token present' });
        }
    );
    // Automatically added sleep
    sleep(1);
}
