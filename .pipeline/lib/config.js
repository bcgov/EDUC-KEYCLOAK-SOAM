'use strict';
const options= require('pipeline-cli').Util.parseArguments()
const changeId = options.pr //aka pull-request
const version = '7.3'
const name = 'sso'

const phases = {
	  build: {namespace:'mvubjx-tools'  , name: `${name}`, phase: 'build', changeId:changeId, suffix: `-build-${changeId}`, instance: `${name}-build-${changeId}`, version:`${version}-${changeId}`, tag:`${version}-${changeId}`},
	  sbox: {namespace:'mvubjx-tools', name: `${name}`, phase: 'sbox' , changeId:changeId, suffix: `-sbox-${changeId}` , instance: `${name}-sbox-${changeId}` , version:`${version}-${changeId}`, tag:`sbox-${version}-${changeId}`, host: `mvubjx-${changeId}-devops-sso-sandbox.pathfinder.gov.bc.ca`},
	  dev: {namespace:'mvubjx-dev'    , name: `${name}`, phase: 'dev'  , changeId:changeId, suffix: '-dev'              , instance: `${name}-dev`              , version:`${version}-${changeId}`, tag:`dev-${version}`             , host: 'mvubjx-dev.pathfinder.gov.bc.ca'},
	  test: {namespace:'mvubjx-test'   , name: `${name}`, phase: 'test' , changeId:changeId, suffix: '-test'             , instance: `${name}-test`             , version:`${version}-${changeId}`, tag:`test-${version}`            , host: 'mvubjx-test.pathfinder.gov.bc.ca'},
	  prod: {namespace:'mvubjx-prod'   , name: `${name}`, phase: 'prod' , changeId:changeId, suffix: '-prod'             , instance: `${name}-prod`             , version:`${version}-${changeId}`, tag:`prod-${version}`            , host: 'mvubjx.pathfinder.gov.bc.ca'}
}

process.on('unhandledRejection', (reason) => {
  console.log(reason);
  process.exit(1);
});

module.exports = exports = {phases, options};