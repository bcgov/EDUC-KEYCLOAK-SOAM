import {sleep, group, check} from "k6";
import * as http from "k6/http";
import {Rate} from "k6/metrics";

export const options = {
    stages: [
        { duration: "1m", target: 60 },
        { duration: "1m", target: 60 },
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

function checkStatus(response, checkName, statusCode = 200) {
    let success = check(response, {
        [checkName]: (r) => {
            if(r.status === statusCode){
                return true
            } else {
                console.debug(checkName + ' failed. Incorrect response code.');
                return false;
            }
        }
    });
    errorRate.add(!success, {tag1: checkName});
}

//Used to get a different user for each test
let userIdx = 0;

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
                    redirects: 2,
                    tags: {
                        name: config.soam.authPath
                    }
                }
            );
            checkStatus(response, config.soam.authPath, 302);
        }
    );
    group(
        "IDP Pages",
        function () {
            response = http.get('https://idtest.gov.bc.ca/login/entry',
                {
                    responseType: 'text',
                    tags: {
                        name: '/login/entry'
                    }
                });

            vars["validateCardResult"] = response
                .html()
                .find("input[name=validateCardResult]")
                .first()
                .attr("value");

            let transactionId;

            let entryCheck = check(response, {
                '/login/entry': (r) => {
                    if (r.body) {
                        transactionId = response.body.match("'cardtap-target-div', '([^']+)'");
                        if (transactionId)
                            transactionId = transactionId[1];
                        else {
                            console.debug('/login/entry failed. No transactionId.');
                            return false;
                        }
                        if (transactionId && vars['validateCardResult'] && r.status === 200) {
                            return true;
                        } else {
                            console.debug('/login/entry failed. Incorrect response body.');
                            return false;
                        }
                    } else {
                        console.debug('/login/entry failed. No response body.');
                        return false;
                    }

                }
            });
            errorRate.add(!entryCheck, {tag1: '/login/entry'});

            response = http.get("https://idtest.gov.bc.ca/cardtap/resources/templates.html?v=R2.9.2",
                {
                    tags: {
                        name: '/cardtap/resources/templates.html'
                    }
                });
            checkStatus(response, '/cardtap/resources/templates.html status');

            response = http.post(
                "https://idtest.gov.bc.ca/cardtap/v3/transactions/" + transactionId + "?clientId=" + config.idim.clientId,
                null,
                {
                    responseType: 'text',
                    headers: {
                        "content-type": "application/json"
                    },
                    tags: {
                        name: '/cardtap/v3/transactions/{transaction-id}'
                    }
                }
            );
            let responseCheck = check(response, {
                "/cardtap/v3/transactions/{transaction-id}": (r) => {
                    if (r.body && r.status === 200) {
                        if (r.body.match('transactionId":"[^"]+"'))
                            return true;
                        else {
                            console.debug('/cardtap/v3/transactions/{transaction-id} failed. No transaction id');
                            return false;
                        }
                    } else {
                        console.debug('/cardtap/v3/transactions/{transaction-id} failed. Incorrect response.');
                        return false;
                    }

                }
            });
            errorRate.add(!responseCheck, {tag1: '/cardtap/v3/transactions/{transaction-id}'});

            response = http.put(
                "https://idtest.gov.bc.ca/cardtap/v3/transactions/" + transactionId + "/device",
                '{"deviceType":"MOCKSKAP"}',
                {
                    responseType: 'text',
                    headers: {
                        "content-type": "application/json"
                    },
                    tags: {
                        name: '/cardtap/v3/transactions/{transaction-id}/device'
                    }
                }
            );

            responseCheck = check(response, {
                "/cardtap/v3/transactions/{transaction-id}/device": (r) => {
                    if (r.body && r.status === 200) {
                        if (r.body.match('transactionId":"[^"]+"'))
                            return true;
                        else {
                            console.debug('/cardtap/v3/transactions/{transaction-id}/device failed. No transaction id.');
                            return false;
                        }
                    } else {
                        console.debug('/cardtap/v3/transactions/{transaction-id}/device failed. Incorrect response.');
                        return false;
                    }

                }
            });
            errorRate.add(!responseCheck, {tag1: '/cardtap/v3/transactions/{transaction-id}/device'});

            response = http.post(
                "https://idtest.gov.bc.ca/MockSKAP/authorize",
                null,
                {
                    tags: {
                        name: '/MockSKAP/authorize'
                    }
                }
            );
            checkStatus(response, '/MockSKAP/authorize status');

            response = http.get("https://idtest.gov.bc.ca/MockSKAP/Widget/mockConnect.html",
                {
                    tags: {
                        name: '/MockSKAP/Widget/mockConnect.html'
                    }
                });
            checkStatus(response, '/MockSKAP/Widget/mockConnect.html status');

            response = http.post(
                "https://idtest.gov.bc.ca/MockSKAP/lookup-mbun-by-csn?csn=" + getUserName(),
                null,
                {
                    headers: {
                        "content-type": "application/json"
                    },
                    responseType: 'text',
                    tags: {
                        name: '/MockSKAP/lookup-mbun-by-csn'
                    }
                }
            );

            let userId;

            responseCheck = check(response, {
                "/MockSKAP/lookup-mbun-by-csn": (r) => {
                    if (r.body && r.status === 200) {
                        userId = response.body.match('mbun":"([^"]*)"');
                        if (userId)
                            userId = userId[1];
                        else {
                            console.debug('/MockSKAP/lookup-mbun-by-csn failed. No userId.');
                            return false;
                        }
                        if (userId && vars['validateCardResult'])
                            return true;
                        else {
                            console.debug('/MockSKAP/lookup-mbun-by-csn failed. Incorrect response body.');
                            return false;
                        }
                    } else {
                        console.debug('/MockSKAP/lookup-mbun-by-csn failed. No response body.');
                        return false;
                    }
                }
            });
            errorRate.add(!responseCheck, {tag1: '/MockSKAP/lookup-mbun-by-csn'});

            response = http.post(
                "https://idtest.gov.bc.ca/MockSKAP/cardread",
                '{"userId": ' + userId + '}',
                {
                    headers: {
                        "content-type": "application/json"
                    },
                    responseType: 'text',
                    tags: {
                        name: '/MockSKAP/cardread'
                    }
                }
            );

            let jwe;
            responseCheck = check(response, {
                '/MockSKAP/cardread': (r) => {
                    if (r.body && r.status === 200) {
                        jwe = response.body.match('JWE":"([^"]*)"');
                        if (jwe) {
                            jwe = jwe[1];
                        } else {
                            console.debug('/MockSKAP/cardread call failed. JWE was not present.')
                        }
                        return jwe;
                    } else {
                        console.debug('/MockSKAP/cardread call failed. Response was incorrect.')
                    }

                }
            });
            errorRate.add(!responseCheck, {tag1: '/MockSKAP/cardread'});

            response = http.put(
                "https://idtest.gov.bc.ca/cardtap/v3/transactions/" + transactionId + "/jwe",
                '{"audience":"mockskap.app","JWE":"' + jwe + '"}',
                {
                    headers: {
                        "content-type": "application/json"
                    },
                    responseType: 'text',
                    tags: {
                        name: '/cardtap/v3/transactions/{transaction-id}/jwe'
                    }
                }
            );

            let validateCardResultFromJwe = response.body;
            responseCheck = check(response, {
                '/cardtap/v3/transactions/{transaction-id}/jwe': (r) => {
                    if (r.body && r.status === 200) {
                        if (validateCardResultFromJwe.includes('"cardStatus":"ACTIVE","cardEventType":"REGISTRATION"'))
                            return true;
                        else {
                            console.debug('/cardtap/v3/transactions/{transaction-id}/jwe call failed. Response body was incorrect.');
                            return false
                        }
                    } else {
                        console.debug('/cardtap/v3/transactions/{transaction-id}/jwe call failed. Response was incorrect.');
                    }

                }
            });
            errorRate.add(!responseCheck, {tag1: '/cardtap/v3/transactions/{transaction-id}/jwe'});

            response = http.post(
                "https://idtest.gov.bc.ca/login/identify",
                {
                    validateCardResult:
                    validateCardResultFromJwe,
                    csrftoken: `${vars["validateCardResult"]}`,
                },
                {

                    responseType: 'text',
                    tags: {
                        name: '/login/identify'
                    }
                }
            );

            responseCheck = check(response, {
                '/login/identify': (r) => {
                    if (r.body && r.status === 200) {
                        if (response.body.match('<strong>' + config.user[userIdx % config.user.length].givenName))
                            return true;
                        else {
                            console.debug('/login/identify call failed. HTML was incorrect.');
                            return false
                        }
                    } else {
                        console.debug('/login/identify call failed. Response was incorrect.');
                    }

                }
            });
            errorRate.add(!responseCheck, {tag1: '/login/identify'});

            response = http.post(
                "https://idtest.gov.bc.ca/login/passcode/validate",
                {
                    csrftoken: `${vars["validateCardResult"]}`,
                    passcode: getUserSecret(),
                },
                {
                    responseType: 'text',
                    tags: {
                        name: '/login/passcode/validate'
                    }
                }
            );

            responseCheck = check(response, {
                '/login/passcode/validate': (r) => {
                    if (r.body && r.status === 200) {
                        if (response.body.match('<strong>' + config.user[userIdx % config.user.length].givenName))
                            return true;
                        else {
                            console.debug('/login/passcode/validate call failed. HTML was incorrect.');
                            return false
                        }
                    } else {
                        console.debug('/login/passcode/validate call failed. Response was incorrect.');
                    }

                }
            });
            errorRate.add(!responseCheck, {tag1: '/login/passcode/validate'});

            response = http.get("https://idtest.gov.bc.ca/login/history",
                {
                    tags: {
                        name: '/login/history'
                    }
                });
            checkStatus(response, '/login/history status');
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
                        name: '/login/setConfirmation'
                    }
                }
            );

            let codeCheck = check(response, {
                "/login/setConfirmation": (r) => {
                    if (r.headers && r.headers.Location && r.status === 302) {
                        if (r.headers.Location.includes("code="))
                            return true;
                        else {
                            console.debug('/login/setConfirmation call failed. Code was not in response.');
                        }
                    } else {
                        console.debug('/login/setConfirmation call failed.  Response was incorrect.');
                        return false;
                    }

                }
            });
            errorRate.add(!codeCheck, {tag1: '/login/setConfirmation'});

        }
    );

    group("SOAM Get Token", function () {

            let code = null;
            if (response.headers && response.headers.Location) {
                code = response.headers.Location.match("code=(.*)");
                if (code) {
                    code = code[1];
                }
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
                        name: config.soam.tokenPath
                    }
                });

            let codeCheck = check(response, {
                [config.soam.tokenPath]: (r) => {
                    if (r.body && r.status === 200) {
                        if (r.body.includes("access_token\":"))
                            return true;
                        else {
                            console.debug(config.soam.tokenPath + ' call failed. Token was not present.');
                            return false;
                        }
                    } else {
                        console.debug(config.soam.tokenPath + ' call failed. Response was incorrect.');
                        return false;
                    }

                }
            });
            errorRate.add(!codeCheck, {tag1: config.soam.tokenPath});
        }
    );
    // Automatically added sleep
    sleep(1);
}
