'use strict';
const options= require('pipeline-cli').Util.parseArguments()
const changeId = options.pr //aka pull-requests
const version = '7.4'
const name = 'sso'

const phases = {
	  build: {namespace:'75e61b-tools'  , name: `${name}`, phase: 'build', changeId:changeId, suffix: `-build-${changeId}`, instance: `${name}-build-${changeId}`, version:`${version}-${changeId}`, tag:`${version}-${changeId}`},
	  sbox: {namespace:'75e61b-tools', name: `${name}`, phase: 'sbox' , changeId:changeId, suffix: `-sbox-${changeId}` , instance: `${name}-sbox-${changeId}` , version:`${version}-${changeId}`, tag:`sbox-${version}-${changeId}`, host: `75e61b-${changeId}-devops-sso-sandbox.pathfinder.gov.bc.ca`},
	  tools: {namespace:'75e61b-tools'    , name: `${name}`, phase: 'tools'  , changeId:changeId, suffix: '-tools'              , instance: `${name}-tools`              , version:`${version}-${changeId}`, tag:`tools-${version}`             , host: 'soam-tools.apps.silver.devops.gov.bc.ca'},
	  dev: {namespace:'75e61b-dev'    , name: `${name}`, phase: 'dev'  , changeId:changeId, suffix: '-dev'              , instance: `${name}-dev`              , version:`${version}-${changeId}`, tag:`dev-${version}`             , host: 'soam-dev.apps.silver.devops.gov.bc.ca'},
	  test: {namespace:'75e61b-test'   , name: `${name}`, phase: 'test' , changeId:changeId, suffix: '-test'             , instance: `${name}-test`             , version:`${version}-${changeId}`, tag:`test-${version}`            , host: 'soam-test.apps.silver.devops.gov.bc.ca'},
	  prod: {namespace:'75e61b-prod'   , name: `${name}`, phase: 'prod' , changeId:changeId, suffix: '-prod'             , instance: `${name}-prod`             , version:`${version}-${changeId}`, tag:`prod-${version}`            , host: 'soam-prod.apps.silver.devops.gov.bc.ca'}
}

process.on('unhandledRejection', (reason) => {
  console.log(reason);
  process.exit(1);
});

module.exports = exports = {phases, options};
