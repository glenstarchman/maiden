/**
 * Wait until the test condition is true or a timeout occurs. Useful for waiting
 * on a server response or for a ui change (fadeIn, etc.) to occur.
 *
 * @param testFx javascript condition that evaluates to a boolean,
 * it can be passed in as a string (e.g.: "1 == 1" or "$('#bar').is(':visible')" or
 * as a callback function.
 * @param onReady what to do when testFx condition is fulfilled,
 * it can be passed in as a string (e.g.: "1 == 1" or "$('#bar').is(':visible')" or
 * as a callback function.
 * @param timeOutMillis the max amount of time to wait. If not specified, 3 sec is used.
 */
var system = require("system");
var args = system.args
var page = require('webpage').create();
var fs = require("fs");
var url = args[1];
var fileName = args[2];
var action = args[3]; //screenshot|generate

function waitFor(testFx, onReady, timeOutMillis) {
  var maxtimeOutMillis = timeOutMillis ? timeOutMillis : 30000, //< Default Max Timout is 30s
    start = new Date().getTime(),
    condition = false,
    interval = setInterval(function() {
      if ( (new Date().getTime() - start < maxtimeOutMillis) && !condition ) {
        // If not time-out yet and condition not yet fulfilled
        condition = (typeof(testFx) === "string" ? eval(testFx) : testFx()); //< defensive code
      } else {
        if(!condition) {
          console.log(-1)
          phantom.exit(1);
        } else {
          // Condition fulfilled (timeout and/or condition is 'true')
          typeof(onReady) === "string" ? eval(onReady) : onReady(); //< Do what it's supposed to do once the condition is fulfilled
          clearInterval(interval); //< Stop this interval
        }
      }
    }, 250); //< repeat check every 250ms 
};


//page.settings.resourceTimeout = 10000; //10 seconds
page.onError = function(msg, trace) { };
page.onConsoleMessage = function(msg) { };
page.onResourceError = function(resource) { };
page.onResourceTimeout = function(e) {
  //console.log(e.url + " timed out");
  //phantom.exit(1);
};

page.customHeaders = {
    "Accept-Encoding": "identity"
};
page.settings.localToRemoteUrlAccessEnabled = true;
page.settings.webSecurityEnabled = false;
page.settings.userAgent = 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.73 Safari/537.36';

if (action === "screenshot") {

  page.viewportSize = {
    width: 2048,
    height: 1024 
  };

  page.clipRect = {
    width: 2048,
    height: 1024 
  };
}


page.open(url, function (status) {
  // Check for page load success
  if (status !== "success") {
    console.log("Unable to access network");
  } else {
    waitFor(function() {
      // Check to see if the document has been fully loaded
      return page.evaluate(function() {
        console.log(document.readyState);
        //if (typeof jQuery !== 'undefined') {
        //  return $("document").ready();
        //} else {
          return "document.readyState === 'complete';";
        //}
      });
    }, function() {
       if (action === "screenshot") {
         page.render(fileName);
         console.log(0)
       } else {
         var content = page.content
         //strip all scripts
         var SCRIPT_REGEX = /<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi;
         while (SCRIPT_REGEX.test(content)) {
           content = content.replace(SCRIPT_REGEX, "");
         }
         //console.log so that we can pick it up from scala
         console.log(content);
       }
       phantom.exit();
    });    
  }
});
