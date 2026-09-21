/**
 * Copyright 2018 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// If the loader is already loaded, just stop.
if (!self.define) {
  let registry = {};

  // Used for `eval` and `importScripts` where we can't get script URL by other means.
  // In both cases, it's safe to use a global var because those functions are synchronous.
  let nextDefineUri;

  const singleRequire = (uri, parentUri) => {
    uri = new URL(uri + ".js", parentUri).href;
    return registry[uri] || (
      
        new Promise(resolve => {
          if ("document" in self) {
            const script = document.createElement("script");
            script.src = uri;
            script.onload = resolve;
            document.head.appendChild(script);
          } else {
            nextDefineUri = uri;
            importScripts(uri);
            resolve();
          }
        })
      
      .then(() => {
        let promise = registry[uri];
        if (!promise) {
          throw new Error(`Module ${uri} didn’t register its module`);
        }
        return promise;
      })
    );
  };

  self.define = (depsNames, factory) => {
    const uri = nextDefineUri || ("document" in self ? document.currentScript.src : "") || location.href;
    if (registry[uri]) {
      // Module is already loading or loaded.
      return;
    }
    let exports = {};
    const require = depUri => singleRequire(depUri, uri);
    const specialDeps = {
      module: { uri },
      exports,
      require
    };
    registry[uri] = Promise.all(depsNames.map(
      depName => specialDeps[depName] || require(depName)
    )).then(deps => {
      factory(...deps);
      return exports;
    });
  };
}
define(['./workbox-7e5eb42b'], (function (workbox) { 'use strict';

  importScripts("/sw-share-target.js");
  self.skipWaiting();
  workbox.clientsClaim();
  /**
   * The precacheAndRoute() method efficiently caches and responds to
   * requests for URLs in the manifest.
   * See https://goo.gl/S9QRab
   */
  workbox.precacheAndRoute([{
    "url": "sw-share-target.js",
    "revision": "93dcd73a052ad537b68a4851da347863"
  }, {
    "url": "pwa-maskable-512x512.png",
    "revision": "1140b59c3140c77419c188f0a7dc166b"
  }, {
    "url": "pwa-512x512.png",
    "revision": "a45e2a415b2ddd4a183a1c72f518f530"
  }, {
    "url": "pwa-192x192.png",
    "revision": "f3e5360d49952dfaa2332b9636910118"
  }, {
    "url": "index.html",
    "revision": "506c54905caec6eba478f05a46fd3533"
  }, {
    "url": "icon.svg",
    "revision": "dcb093745496b1b69e5d0ecbafb5554e"
  }, {
    "url": "apple-touch-icon.png",
    "revision": "caafe814e4e1fb17159d46adbce0c05d"
  }, {
    "url": "assets/workbox-window.prod.es5-BBnX5xw4.js",
    "revision": null
  }, {
    "url": "assets/index-DmzPjeOB.css",
    "revision": null
  }, {
    "url": "assets/index-CTc_xbZh.js",
    "revision": null
  }, {
    "url": "apple-touch-icon.png",
    "revision": "caafe814e4e1fb17159d46adbce0c05d"
  }, {
    "url": "icon.svg",
    "revision": "dcb093745496b1b69e5d0ecbafb5554e"
  }, {
    "url": "pwa-192x192.png",
    "revision": "f3e5360d49952dfaa2332b9636910118"
  }, {
    "url": "pwa-512x512.png",
    "revision": "a45e2a415b2ddd4a183a1c72f518f530"
  }, {
    "url": "pwa-maskable-512x512.png",
    "revision": "1140b59c3140c77419c188f0a7dc166b"
  }, {
    "url": "sw-share-target.js",
    "revision": "93dcd73a052ad537b68a4851da347863"
  }, {
    "url": "manifest.webmanifest",
    "revision": "de0e92f38c37b593d1cd09d39d857391"
  }], {});
  workbox.cleanupOutdatedCaches();
  workbox.registerRoute(new workbox.NavigationRoute(workbox.createHandlerBoundToURL("index.html"), {
    denylist: [/^\/share-target/]
  }));

}));
