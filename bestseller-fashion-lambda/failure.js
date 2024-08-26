"use strict";
const { SSMClient, GetParametersCommand } = require("@aws-sdk/client-ssm");
const fetch = require("node-fetch");
const childProcess = require("child_process");
const Mitm = require("mitm");

const ssm = new SSMClient();

let mitm = null;

function clearMitm() {
  if (mitm != null) {
    mitm.disable();
  }
}

async function getConfig() {
  const defaults = {
    isEnabled: false,
  };
  if (process.env.FAILURE_APPCONFIG_CONFIGURATION) {
    try {
      let appconfigPort = 2772;
      if (process.env.AWS_APPCONFIG_EXTENSION_HTTP_PORT) {
        appconfigPort = process.env.AWS_APPCONFIG_EXTENSION_HTTP_PORT;
      }
      const url =
        "http://localhost:" +
        appconfigPort +
        "/applications/" +
        process.env.FAILURE_APPCONFIG_APPLICATION +
        "/environments/" +
        process.env.FAILURE_APPCONFIG_ENVIRONMENT +
        "/configurations/" +
        process.env.FAILURE_APPCONFIG_CONFIGURATION;
      const response = await fetch(url);
      const json = await response.json();
      return json;
    } catch (err) {
      console.error(err);
      return defaults;
    }
  } else if (process.env.FAILURE_INJECTION_PARAM) {
    try {
      const params = {
        Names: [process.env.FAILURE_INJECTION_PARAM],
      };
      const response = await ssm.send(new GetParametersCommand(params));
      if (
        response.InvalidParameters != null &&
        response.InvalidParameters.length > 0
      ) {
        return defaults;
      }
      return JSON.parse(response.Parameters[0].Value);
    } catch (err) {
      console.error(err);
      return defaults;
    }
  } else {
    return defaults;
  }
}

const injectFailure = function (fn) {
  return async function () {
    try {
      const config = await getConfig();

      if (config.isEnabled === false || config.failureMode !== "denylist") {
        clearMitm();
      }

      if (config.isEnabled === true && Math.random() < config.rate) {
        if (config.failureMode === "latency") {
          const latencyRange = config.maxLatency - config.minLatency;
          const setLatency = Math.floor(
            config.minLatency + Math.random() * latencyRange
          );
          console.log("Injecting " + setLatency + " ms latency.");
          await new Promise((resolve) => setTimeout(resolve, setLatency));
        } else if (config.failureMode === "exception") {
          console.log("Injecting exception message: " + config.exceptionMsg);
          throw new Error(config.exceptionMsg);
        } else if (config.failureMode === "statuscode") {
          console.log("Injecting status code: " + config.statusCode);
          return { statusCode: config.statusCode };
        } else if (config.failureMode === "diskspace") {
          console.log("Injecting disk space: " + config.diskSpace + " MB");
          childProcess.spawnSync("dd", [
            "if=/dev/zero",
            "of=/tmp/diskspace-failure-" + Date.now() + ".tmp",
            "count=1000",
            "bs=" + config.diskSpace * 1000,
          ]);
        } else if (config.failureMode === "denylist") {
          console.log(
            "Injecting dependency failure through a network block for denylisted sites: " +
              config.denylist
          );

          try {
            // attach a handler to filter the configured deny patterns
            const blRegexs = [];
            config.denylist.forEach(function (regexStr) {
              blRegexs.push(new RegExp(regexStr));
            });

            if (blRegexs.length !== 0) {
              // if the global mitm doesn't yet exist, create it now
              if (mitm == null) {
                mitm = Mitm();
              }
              mitm.enable();
              mitm.on("connect", function (socket, opts) {
                let block = false;
                blRegexs.forEach(function (blRegex) {
                  if (blRegex.test(opts.host)) {
                    console.log(
                      "Intercepted network connection to " + opts.host
                    );
                    block = true;
                  }
                });
                if (block) {
                  socket.end();
                } else {
                  socket.bypass();
                }
              });

              // remove any previously attached handlers, leaving only the most recently added
              while (typeof mitm._events.connect !== "function") {
                mitm.removeListener("connect", mitm._events.connect[0]);
              }
            }
          } catch (ex) {
            console.log("Error while handling network block failure.", ex);
            clearMitm();
          }
        }
      }
      return fn.apply(this, arguments);
    } catch (ex) {
      console.log(ex);
      throw ex;
    }
  };
};

module.exports = injectFailure;
