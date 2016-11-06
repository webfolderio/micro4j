(function(f){if(typeof exports==="object"&&typeof module!=="undefined"){module.exports=f()}else if(typeof define==="function"&&define.amd){define([],f)}else{var g;if(typeof window!=="undefined"){g=window}else if(typeof global!=="undefined"){g=global}else if(typeof self!=="undefined"){g=self}else{g=this}g.module = f()}})(function(){var define,module,exports;return (function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){

},{}],2:[function(require,module,exports){
(function (process){
// Copyright Joyent, Inc. and other Node contributors.
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.

// resolves . and .. elements in a path array with directory names there
// must be no slashes, empty elements, or device names (c:\) in the array
// (so also no leading and trailing slashes - it does not distinguish
// relative and absolute paths)
function normalizeArray(parts, allowAboveRoot) {
  // if the path tries to go above the root, `up` ends up > 0
  var up = 0;
  for (var i = parts.length - 1; i >= 0; i--) {
    var last = parts[i];
    if (last === '.') {
      parts.splice(i, 1);
    } else if (last === '..') {
      parts.splice(i, 1);
      up++;
    } else if (up) {
      parts.splice(i, 1);
      up--;
    }
  }

  // if the path is allowed to go above the root, restore leading ..s
  if (allowAboveRoot) {
    for (; up--; up) {
      parts.unshift('..');
    }
  }

  return parts;
}

// Split a filename into [root, dir, basename, ext], unix version
// 'root' is just a slash, or nothing.
var splitPathRe =
    /^(\/?|)([\s\S]*?)((?:\.{1,2}|[^\/]+?|)(\.[^.\/]*|))(?:[\/]*)$/;
var splitPath = function(filename) {
  return splitPathRe.exec(filename).slice(1);
};

// path.resolve([from ...], to)
// posix version
exports.resolve = function() {
  var resolvedPath = '',
      resolvedAbsolute = false;

  for (var i = arguments.length - 1; i >= -1 && !resolvedAbsolute; i--) {
    var path = (i >= 0) ? arguments[i] : process.cwd();

    // Skip empty and invalid entries
    if (typeof path !== 'string') {
      throw new TypeError('Arguments to path.resolve must be strings');
    } else if (!path) {
      continue;
    }

    resolvedPath = path + '/' + resolvedPath;
    resolvedAbsolute = path.charAt(0) === '/';
  }

  // At this point the path should be resolved to a full absolute path, but
  // handle relative paths to be safe (might happen when process.cwd() fails)

  // Normalize the path
  resolvedPath = normalizeArray(filter(resolvedPath.split('/'), function(p) {
    return !!p;
  }), !resolvedAbsolute).join('/');

  return ((resolvedAbsolute ? '/' : '') + resolvedPath) || '.';
};

// path.normalize(path)
// posix version
exports.normalize = function(path) {
  var isAbsolute = exports.isAbsolute(path),
      trailingSlash = substr(path, -1) === '/';

  // Normalize the path
  path = normalizeArray(filter(path.split('/'), function(p) {
    return !!p;
  }), !isAbsolute).join('/');

  if (!path && !isAbsolute) {
    path = '.';
  }
  if (path && trailingSlash) {
    path += '/';
  }

  return (isAbsolute ? '/' : '') + path;
};

// posix version
exports.isAbsolute = function(path) {
  return path.charAt(0) === '/';
};

// posix version
exports.join = function() {
  var paths = Array.prototype.slice.call(arguments, 0);
  return exports.normalize(filter(paths, function(p, index) {
    if (typeof p !== 'string') {
      throw new TypeError('Arguments to path.join must be strings');
    }
    return p;
  }).join('/'));
};


// path.relative(from, to)
// posix version
exports.relative = function(from, to) {
  from = exports.resolve(from).substr(1);
  to = exports.resolve(to).substr(1);

  function trim(arr) {
    var start = 0;
    for (; start < arr.length; start++) {
      if (arr[start] !== '') break;
    }

    var end = arr.length - 1;
    for (; end >= 0; end--) {
      if (arr[end] !== '') break;
    }

    if (start > end) return [];
    return arr.slice(start, end - start + 1);
  }

  var fromParts = trim(from.split('/'));
  var toParts = trim(to.split('/'));

  var length = Math.min(fromParts.length, toParts.length);
  var samePartsLength = length;
  for (var i = 0; i < length; i++) {
    if (fromParts[i] !== toParts[i]) {
      samePartsLength = i;
      break;
    }
  }

  var outputParts = [];
  for (var i = samePartsLength; i < fromParts.length; i++) {
    outputParts.push('..');
  }

  outputParts = outputParts.concat(toParts.slice(samePartsLength));

  return outputParts.join('/');
};

exports.sep = '/';
exports.delimiter = ':';

exports.dirname = function(path) {
  var result = splitPath(path),
      root = result[0],
      dir = result[1];

  if (!root && !dir) {
    // No dirname whatsoever
    return '.';
  }

  if (dir) {
    // It has a dirname, strip trailing slash
    dir = dir.substr(0, dir.length - 1);
  }

  return root + dir;
};


exports.basename = function(path, ext) {
  var f = splitPath(path)[2];
  // TODO: make this comparison case-insensitive on windows?
  if (ext && f.substr(-1 * ext.length) === ext) {
    f = f.substr(0, f.length - ext.length);
  }
  return f;
};


exports.extname = function(path) {
  return splitPath(path)[3];
};

function filter (xs, f) {
    if (xs.filter) return xs.filter(f);
    var res = [];
    for (var i = 0; i < xs.length; i++) {
        if (f(xs[i], i, xs)) res.push(xs[i]);
    }
    return res;
}

// String.prototype.substr - negative index don't work in IE8
var substr = 'ab'.substr(-1) === 'b'
    ? function (str, start, len) { return str.substr(start, len) }
    : function (str, start, len) {
        if (start < 0) start = str.length + start;
        return str.substr(start, len);
    }
;

}).call(this,require('_process'))
},{"_process":3}],3:[function(require,module,exports){
// shim for using process in browser
var process = module.exports = {};

// cached from whatever global is present so that test runners that stub it
// don't break things.  But we need to wrap it in a try catch in case it is
// wrapped in strict mode code which doesn't define any globals.  It's inside a
// function because try/catches deoptimize in certain engines.

var cachedSetTimeout;
var cachedClearTimeout;

function defaultSetTimout() {
    throw new Error('setTimeout has not been defined');
}
function defaultClearTimeout () {
    throw new Error('clearTimeout has not been defined');
}
(function () {
    try {
        if (typeof setTimeout === 'function') {
            cachedSetTimeout = setTimeout;
        } else {
            cachedSetTimeout = defaultSetTimout;
        }
    } catch (e) {
        cachedSetTimeout = defaultSetTimout;
    }
    try {
        if (typeof clearTimeout === 'function') {
            cachedClearTimeout = clearTimeout;
        } else {
            cachedClearTimeout = defaultClearTimeout;
        }
    } catch (e) {
        cachedClearTimeout = defaultClearTimeout;
    }
} ())
function runTimeout(fun) {
    if (cachedSetTimeout === setTimeout) {
        //normal enviroments in sane situations
        return setTimeout(fun, 0);
    }
    // if setTimeout wasn't available but was latter defined
    if ((cachedSetTimeout === defaultSetTimout || !cachedSetTimeout) && setTimeout) {
        cachedSetTimeout = setTimeout;
        return setTimeout(fun, 0);
    }
    try {
        // when when somebody has screwed with setTimeout but no I.E. maddness
        return cachedSetTimeout(fun, 0);
    } catch(e){
        try {
            // When we are in I.E. but the script has been evaled so I.E. doesn't trust the global object when called normally
            return cachedSetTimeout.call(null, fun, 0);
        } catch(e){
            // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error
            return cachedSetTimeout.call(this, fun, 0);
        }
    }


}
function runClearTimeout(marker) {
    if (cachedClearTimeout === clearTimeout) {
        //normal enviroments in sane situations
        return clearTimeout(marker);
    }
    // if clearTimeout wasn't available but was latter defined
    if ((cachedClearTimeout === defaultClearTimeout || !cachedClearTimeout) && clearTimeout) {
        cachedClearTimeout = clearTimeout;
        return clearTimeout(marker);
    }
    try {
        // when when somebody has screwed with setTimeout but no I.E. maddness
        return cachedClearTimeout(marker);
    } catch (e){
        try {
            // When we are in I.E. but the script has been evaled so I.E. doesn't  trust the global object when called normally
            return cachedClearTimeout.call(null, marker);
        } catch (e){
            // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error.
            // Some versions of I.E. have different rules for clearTimeout vs setTimeout
            return cachedClearTimeout.call(this, marker);
        }
    }



}
var queue = [];
var draining = false;
var currentQueue;
var queueIndex = -1;

function cleanUpNextTick() {
    if (!draining || !currentQueue) {
        return;
    }
    draining = false;
    if (currentQueue.length) {
        queue = currentQueue.concat(queue);
    } else {
        queueIndex = -1;
    }
    if (queue.length) {
        drainQueue();
    }
}

function drainQueue() {
    if (draining) {
        return;
    }
    var timeout = runTimeout(cleanUpNextTick);
    draining = true;

    var len = queue.length;
    while(len) {
        currentQueue = queue;
        queue = [];
        while (++queueIndex < len) {
            if (currentQueue) {
                currentQueue[queueIndex].run();
            }
        }
        queueIndex = -1;
        len = queue.length;
    }
    currentQueue = null;
    draining = false;
    runClearTimeout(timeout);
}

process.nextTick = function (fun) {
    var args = new Array(arguments.length - 1);
    if (arguments.length > 1) {
        for (var i = 1; i < arguments.length; i++) {
            args[i - 1] = arguments[i];
        }
    }
    queue.push(new Item(fun, args));
    if (queue.length === 1 && !draining) {
        runTimeout(drainQueue);
    }
};

// v8 likes predictible objects
function Item(fun, array) {
    this.fun = fun;
    this.array = array;
}
Item.prototype.run = function () {
    this.fun.apply(null, this.array);
};
process.title = 'browser';
process.browser = true;
process.env = {};
process.argv = [];
process.version = ''; // empty string to avoid regexp issues
process.versions = {};

function noop() {}

process.on = noop;
process.addListener = noop;
process.once = noop;
process.off = noop;
process.removeListener = noop;
process.removeAllListeners = noop;
process.emit = noop;

process.binding = function (name) {
    throw new Error('process.binding is not supported');
};

process.cwd = function () { return '/' };
process.chdir = function (dir) {
    throw new Error('process.chdir is not supported');
};
process.umask = function() { return 0; };

},{}],4:[function(require,module,exports){
(function (process){
/**
 * UglifyCSS
 * Port of YUI CSS Compressor to NodeJS
 * Author: Franck Marcia - https://github.com/fmarcia
 * MIT licenced
 */

/**
 * cssmin.js
 * Author: Stoyan Stefanov - http://phpied.com/
 * This is a JavaScript port of the CSS minification tool
 * distributed with YUICompressor, itself a port
 * of the cssmin utility by Isaac Schlueter - http://foohack.com/
 * Permission is hereby granted to use the JavaScript version under the same
 * conditions as the YUICompressor (original YUICompressor note below).
 */

/**
 * YUI Compressor
 * http://developer.yahoo.com/yui/compressor/
 * Author: Julien Lecomte - http://www.julienlecomte.net/
 * Copyright (c) 2011 Yahoo! Inc. All rights reserved.
 * The copyrights embodied in the content of this file are licensed
 * by Yahoo! Inc. under the BSD (revised) open source license.
 */

'use strict';

var fs = require('fs');
var path = require('path');

var SEP = "/";
var PATH_SEP = path.sep;
var ___PRESERVED_TOKEN_ = "___PRESERVED_TOKEN_";

var defaultOptions = {
    maxLineLen: 0,
    expandVars: false,
    uglyComments: false,
    cuteComments: false,
    convertUrls: "",
    debug: false
};

/**
 * Utility method to convert relative urls and replace them with tokens before
 * we start compressing. It must be called *after* extractDataUrls
 *
 * @private
 * @function convertRelativeUrls
 * @param {String} css The input css
 * @param {Object} options Options
 * @param {Array} The global array of tokens to preserve
 * @returns String The processed css
 */
function convertRelativeUrls(css, options, preservedTokens) {

    var maxIndex = css.length - 1,
        appendIndex = 0,
        startIndex,
        endIndex,
        terminator,
        foundTerminator,
        sb = [],
        m,
        preserver,
        token,
        url,
        file,
        target,
        pattern = /(url\s*\()\s*(["']?)/g;

    // Since we need to account for non-base64 data urls, we need to handle
    // ' and ) being part of the data string. Hence switching to indexOf,
    // to determine whether or not we have matching string terminators and
    // handling sb appends directly, instead of using matcher.append* methods.

    while ((m = pattern.exec(css)) !== null) {

        startIndex = m.index + m[1].length;  // "url(".length()
        terminator = m[2];         // ', " or empty (not quoted)

        if (terminator.length === 0) {
            terminator = ")";
        }

        foundTerminator = false;

        endIndex = pattern.lastIndex - 1;

        while(foundTerminator === false && endIndex+1 <= maxIndex) {
            endIndex = css.indexOf(terminator, endIndex + 1);

            // endIndex == 0 doesn't really apply here
            if ((endIndex > 0) && (css.charAt(endIndex - 1) !== '\\')) {
                foundTerminator = true;
                if (")" != terminator) {
                    endIndex = css.indexOf(")", endIndex);
                }
            }
        }

        // Enough searching, start moving stuff over to the buffer
        sb.push(css.substring(appendIndex, m.index));

        if (foundTerminator) {

            token = css.substring(startIndex, endIndex).replace(/(^\s*|\s*$)/g, "");
            if (token.slice(0, 19) !== ___PRESERVED_TOKEN_) {

                if (terminator === "'" || terminator === '"') {
                    token = token.slice(1, -1);
                } else if (terminator === ")") {
                    terminator = "";
                }

                if (options.convertUrls && token.charAt(0) !== SEP && token.slice(0, 7) !== "http://" && token.slice(0, 8) !== "https://") {

                    // build path of detected urls
                    target = options.target.slice();
                    token = token.split(SEP).join(PATH_SEP); // assuming urls in css use "/"
                    url = path.resolve(options.source.join(PATH_SEP), token).split(PATH_SEP);

                    file = url.pop();

                    // remove common part of both paths
                    while (target[0] === url[0]) {
                        target.shift();
                        url.shift();
                    }

                    for (var i = 0, l = target.length; i < l; ++i) {
                        target[i] = "..";
                    }
                    url = terminator + target.concat(url, file).join(SEP) + terminator;

                } else {
                    url = terminator + token + terminator;
                }

                preservedTokens.push(url);

                preserver = "url(" + ___PRESERVED_TOKEN_ + (preservedTokens.length - 1) + "___)";
                sb.push(preserver);

            } else {
                sb.push("url(" + token + ")");
            }

            appendIndex = endIndex + 1;

        } else {
            // No end terminator found, re-add the whole match. Should we throw/warn here?
            sb.push(css.substring(m.index, pattern.lastIndex));
            appendIndex = pattern.lastIndex;
        }
    }

    sb.push(css.substring(appendIndex));

    return sb.join("");
}

/**
 * Utility method to replace all data urls with tokens before we start
 * compressing, to avoid performance issues running some of the subsequent
 * regexes against large strings chunks.
 *
 * @private
 * @function extractDataUrls
 * @param {String} css The input css
 * @param {Array} The global array of tokens to preserve
 * @returns String The processed css
 */
function extractDataUrls(css, preservedTokens) {

    // Leave data urls alone to increase parse performance.
    var maxIndex = css.length - 1,
        appendIndex = 0,
        startIndex,
        endIndex,
        terminator,
        foundTerminator,
        sb = [],
        m,
        preserver,
        token,
        pattern = /url\(\s*(["']?)data\:/g;

    // Since we need to account for non-base64 data urls, we need to handle
    // ' and ) being part of the data string. Hence switching to indexOf,
    // to determine whether or not we have matching string terminators and
    // handling sb appends directly, instead of using matcher.append* methods.

    while ((m = pattern.exec(css)) !== null) {

        startIndex = m.index + 4;  // "url(".length()
        terminator = m[1];         // ', " or empty (not quoted)

        if (terminator.length === 0) {
            terminator = ")";
        }

        foundTerminator = false;

        endIndex = pattern.lastIndex - 1;

        while(foundTerminator === false && endIndex+1 <= maxIndex) {
            endIndex = css.indexOf(terminator, endIndex + 1);

            // endIndex == 0 doesn't really apply here
            if ((endIndex > 0) && (css.charAt(endIndex - 1) !== '\\')) {
                foundTerminator = true;
                if (")" != terminator) {
                    endIndex = css.indexOf(")", endIndex);
                }
            }
        }

        // Enough searching, start moving stuff over to the buffer
        sb.push(css.substring(appendIndex, m.index));

        if (foundTerminator) {

            token = css.substring(startIndex, endIndex);
            var parts = token.split(",");
            if (parts.length > 1 && parts[0].slice(-7) == ";base64") {
                token = token.replace(/\s+/g, "");
            } else {
                token = token.replace(/\n/g, " ");
                token = token.replace(/\s+/g, " ");
                token = token.replace(/(^\s+|\s+$)/g, "");
            }

            preservedTokens.push(token);

            preserver = "url(" + ___PRESERVED_TOKEN_ + (preservedTokens.length - 1) + "___)";
            sb.push(preserver);

            appendIndex = endIndex + 1;
        } else {
            // No end terminator found, re-add the whole match. Should we throw/warn here?
            sb.push(css.substring(m.index, pattern.lastIndex));
            appendIndex = pattern.lastIndex;
        }
    }

    sb.push(css.substring(appendIndex));

    return sb.join("");
}

/**
 * Utility method to compress hex color values of the form #AABBCC to #ABC.
 *
 * DOES NOT compress CSS ID selectors which match the above pattern (which would break things).
 * e.g. #AddressForm { ... }
 *
 * DOES NOT compress IE filters, which have hex color values (which would break things).
 * e.g. filter: chroma(color="#FFFFFF");
 *
 * DOES NOT compress invalid hex values.
 * e.g. background-color: #aabbccdd
 *
 * @private
 * @function compressHexColors
 * @param {String} css The input css
 * @returns String The processed css
 */
function compressHexColors(css) {

    // Look for hex colors inside { ... } (to avoid IDs) and which don't have a =, or a " in front of them (to avoid filters)
    var pattern = /(\=\s*?["']?)?#([0-9a-f])([0-9a-f])([0-9a-f])([0-9a-f])([0-9a-f])([0-9a-f])(\}|[^0-9a-f{][^{]*?\})/gi,
        m,
        index = 0,
        isFilter,
        sb = [];

    while ((m = pattern.exec(css)) !== null) {

        sb.push(css.substring(index, m.index));

        isFilter = m[1];

        if (isFilter) {
            // Restore, maintain case, otherwise filter will break
            sb.push(m[1] + "#" + (m[2] + m[3] + m[4] + m[5] + m[6] + m[7]));
        } else {
            if (m[2].toLowerCase() == m[3].toLowerCase() &&
                m[4].toLowerCase() == m[5].toLowerCase() &&
                m[6].toLowerCase() == m[7].toLowerCase()) {

                // Compress.
                sb.push("#" + (m[3] + m[5] + m[7]).toLowerCase());
            } else {
                // Non compressible color, restore but lower case.
                sb.push("#" + (m[2] + m[3] + m[4] + m[5] + m[6] + m[7]).toLowerCase());
            }
        }

        index = pattern.lastIndex = pattern.lastIndex - m[8].length;
    }

    sb.push(css.substring(index));

    return sb.join("");
}

// Preserve 0 followed by unit in keyframes steps

function keyframes(content, preservedTokens) {

    var level,
        buffer,
        buffers,
        pattern = /@[a-z0-9-_]*keyframes\s+[a-z0-9-_]+\s*{/gi,
        index = 0,
        len,
        c,
        startIndex;

    var preserve = function (part, index) {
        part = part.replace(/(^\s|\s$)/g, '');
        if (part.charAt(0) === '0') {
            preservedTokens.push(part);
            buffer[index] = ___PRESERVED_TOKEN_ + (preservedTokens.length - 1) + "___";
        }
    };

    while (true) {

        level = 0;
        buffer = '';

        startIndex = content.slice(index).search(pattern);
        if (startIndex < 0) {
            break;
        }

        index += startIndex;
        startIndex = index;
        len = content.length;
        buffers = [];

        for (; index < len; ++index) {

            c = content.charAt(index);

            if (c === '{') {

                if (level === 0) {
                    buffers.push(buffer.replace(/(^\s|\s$)/g, ''));

                } else if (level === 1) {

                    buffer = buffer.split(',');

                    buffer.forEach(preserve);

                    buffers.push(buffer.join(',').replace(/(^\s|\s$)/g, ''));
                }

                buffer = '';
                level += 1;

            } else if (c === '}') {

                if (level === 2) {
                    buffers.push('{' + buffer.replace(/(^\s|\s$)/g, '') + '}');
                    buffer = '';

                } else if (level === 1) {
                    content = content.slice(0, startIndex) +
                        buffers.shift() + '{' +
                        buffers.join('') +
                        content.slice(index);
                    break;
                }

                level -= 1;
            }

            if (level < 0) {
                break;

            } else if (c !== '{' && c !== '}') {
                buffer += c;
            }
        }
    }

    return content;
}

// Collect all comment blocks and return new content with comment placeholders
// (comments is an array thus passed by reference)

function collectComments(content, comments) {
    var table = [];
    var from = 0;
    var start, end;
    while (true) {
        start = content.indexOf("/*", from);
        if (start > -1) {
            end = content.indexOf("*/", start + 2);
            if (end > -1) {
                comments.push(content.slice(start + 2, end));
                table.push(content.slice(from, start));
                table.push("/*___PRESERVE_CANDIDATE_COMMENT_" + (comments.length - 1) + "___*/");
                from = end + 2;
            } else {
                // unterminated comment
                end = -2;
                break;
            }
        } else {
            break;
        }
    }
    table.push(content.slice(end + 2));
    return table.join("");
}

// Uglify a CSS string

function processString(content, options) {

    var startIndex,
        comments = [],
        preservedTokens = [],
        token,
        len = content.length,
        pattern,
        quote,
        rgbcolors,
        hexcolor,
        placeholder,
        val,
        i,
        c,
        line = [],
        lines = [],
        vars = {};

    options = options || defaultOptions;
    content = extractDataUrls(content, preservedTokens);
    content = convertRelativeUrls(content, options, preservedTokens);
    content = collectComments(content, comments);

    // preserve strings so their content doesn't get accidentally minified
    pattern = /("([^\\"]|\\.|\\)*")|('([^\\']|\\.|\\)*')/g;
    content = content.replace(pattern, function (token) {
        quote = token.substring(0, 1);
        token = token.slice(1, -1);
        // maybe the string contains a comment-like substring or more? put'em back then
        if (token.indexOf("___PRESERVE_CANDIDATE_COMMENT_") >= 0) {
            for (i = 0, len = comments.length; i < len; i += 1) {
                token = token.replace("___PRESERVE_CANDIDATE_COMMENT_" + i + "___", comments[i]);
            }
        }
        // minify alpha opacity in filter strings
        token = token.replace(/progid:DXImageTransform.Microsoft.Alpha\(Opacity=/gi, "alpha(opacity=");
        preservedTokens.push(token);
        return quote + ___PRESERVED_TOKEN_ + (preservedTokens.length - 1) + "___" + quote;
    });

    // strings are safe, now wrestle the comments
    for (i = 0, len = comments.length; i < len; i += 1) {

        token = comments[i];
        placeholder = "___PRESERVE_CANDIDATE_COMMENT_" + i + "___";

        // ! in the first position of the comment means preserve
        // so push to the preserved tokens keeping the !
        if (token.charAt(0) === "!") {
            if (options.cuteComments) {
                preservedTokens.push(token.substring(1));
            } else if (options.uglyComments) {
                preservedTokens.push(token.substring(1).replace(/[\r\n]/g, ''));
            } else {
                preservedTokens.push(token);
            }
            content = content.replace(placeholder, ___PRESERVED_TOKEN_ + (preservedTokens.length - 1) + "___");
            continue;
        }

        // \ in the last position looks like hack for Mac/IE5
        // shorten that to /*\*/ and the next one to /**/
        if (token.charAt(token.length - 1) === "\\") {
            preservedTokens.push("\\");
            content = content.replace(placeholder, ___PRESERVED_TOKEN_ + (preservedTokens.length - 1) + "___");
            i = i + 1; // attn: advancing the loop
            preservedTokens.push("");
            content = content.replace(
                "___PRESERVE_CANDIDATE_COMMENT_" + i + "___",
                ___PRESERVED_TOKEN_ + (preservedTokens.length - 1) + "___"
            );
            continue;
        }

        // keep empty comments after child selectors (IE7 hack)
        // e.g. html >/**/ body
        if (token.length === 0) {
            startIndex = content.indexOf(placeholder);
            if (startIndex > 2) {
                if (content.charAt(startIndex - 3) === '>') {
                    preservedTokens.push("");
                    content = content.replace(placeholder, ___PRESERVED_TOKEN_ + (preservedTokens.length - 1) + "___");
                }
            }
        }

        // in all other cases kill the comment
        content = content.replace("/*" + placeholder + "*/", "");
    }

    if (options.expandVars) {
        // parse simple @variables blocks and remove them
        pattern = /@variables\s*\{\s*([^\}]+)\s*\}/g;
        content = content.replace(pattern, function (ignore, f1) {
            pattern = /\s*([a-z0-9\-]+)\s*:\s*([^;\}]+)\s*/gi;
            f1.replace(pattern, function (ignore, f1, f2) {
                if (f1 && f2) {
                vars[f1] = f2;
                }
                return '';
            });
            return '';
        });

        // replace var(x) with the value of x
        pattern = /var\s*\(\s*([^\)]+)\s*\)/g;
        content = content.replace(pattern, function (ignore, f1) {
            return vars[f1] || 'none';
        });
    }

    // normalize all whitespace strings to single spaces. Easier to work with that way.
    content = content.replace(/\s+/g, " ");

    // preserve formulas in calc() before removing spaces
    pattern = /calc\(([^;}]*)\)/g;
    content = content.replace(pattern, function (ignore, f1) {
        preservedTokens.push(
            'calc(' +
                f1.replace(/(^\s*|\s*$)/g, "")
                  .replace(/\( /g, "(")
                  .replace(/ \)/g, ")") +
            ')'
        );
        return ___PRESERVED_TOKEN_ + (preservedTokens.length - 1) + "___";
    });

    // preserve matrix
    pattern = /\s*filter:\s*progid:DXImageTransform.Microsoft.Matrix\(([^\)]+)\);/g;
    content = content.replace(pattern, function (ignore, f1) {
        preservedTokens.push(f1);
        return "filter:progid:DXImageTransform.Microsoft.Matrix(" + ___PRESERVED_TOKEN_ + (preservedTokens.length - 1) + "___);";
    });

    // remove the spaces before the things that should not have spaces before them.
    // but, be careful not to turn "p :link {...}" into "p:link{...}"
    // swap out any pseudo-class colons with the token, and then swap back.
    pattern = /(^|\})(([^\{:])+:)+([^\{]*\{)/g;
    content = content.replace(pattern, function (token) {
        return token.replace(/:/g, "___PSEUDOCLASSCOLON___");
    });

    // remove spaces before the things that should not have spaces before them.
    content = content.replace(/\s+([!{};:>+\(\)\],])/g, "$1");

    // restore spaces for !important
    content = content.replace(/!important/g, " !important");

    // bring back the colon
    content = content.replace(/___PSEUDOCLASSCOLON___/g, ":");

    // preserve 0 followed by a time unit for properties using time units
    pattern = /\s*(animation|animation-delay|animation-duration|transition|transition-delay|transition-duration):\s*([^;}]+)/gi;
    content = content.replace(pattern, function (ignore, f1, f2) {

        f2 = f2.replace(/(^|\D)0?\.?0(m?s)/gi, function (ignore, g1, g2) {
            preservedTokens.push('0' + g2);
            return g1 + ___PRESERVED_TOKEN_ + (preservedTokens.length - 1) + "___";
        });

        return f1 + ":" + f2;
    });

    // preserve 0% in hsl and hsla color definitions
    content = content.replace(/(hsla?)\(([^)]+)\)/g, function (ignore, f1, f2) {
        var f0 = [];
        f2.split(',').forEach(function (part) {
            part = part.replace(/(^\s+|\s+$)/g, "");
            if (part === '0%') {
                preservedTokens.push('0%');
                f0.push(___PRESERVED_TOKEN_ + (preservedTokens.length - 1) + "___");
            } else {
                f0.push(part);
            }
        });
        return f1 + '(' + f0.join(',') + ')';
    });

    // preserve 0 followed by unit in keyframes steps (WIP)
    content = keyframes(content, preservedTokens);

    // retain space for special IE6 cases
    content = content.replace(/:first-(line|letter)(\{|,)/gi, function (ignore, f1, f2) {
        return ":first-" + f1.toLowerCase() + " " + f2;
    });

    // newlines before and after the end of a preserved comment
    if (options.cuteComments) {
        content = content.replace(/\s*\/\*/g, "___PRESERVED_NEWLINE___/*");
        content = content.replace(/\*\/\s*/g, "*/___PRESERVED_NEWLINE___");
    // no space after the end of a preserved comment
    } else {
        content = content.replace(/\*\/\s*/g, '*/');
    }

    // If there are multiple @charset directives, push them to the top of the file.
    pattern = /^(.*)(@charset)( "[^"]*";)/gi;
    content = content.replace(pattern, function (ignore, f1, f2, f3) {
        return f2.toLowerCase() + f3 + f1;
    });

    // When all @charset are at the top, remove the second and after (as they are completely ignored).
    pattern = /^((\s*)(@charset)( [^;]+;\s*))+/gi;
    content = content.replace(pattern, function (ignore, ignore2, f2, f3, f4) {
        return f2 + f3.toLowerCase() + f4;
    });

    // lowercase some popular @directives (@charset is done right above)
    pattern = /@(font-face|import|(?:-(?:atsc|khtml|moz|ms|o|wap|webkit)-)?keyframe|media|page|namespace)/gi;
    content = content.replace(pattern, function (ignore, f1) {
        return '@' + f1.toLowerCase();
    });

    // lowercase some more common pseudo-elements
    pattern = /:(active|after|before|checked|disabled|empty|enabled|first-(?:child|of-type)|focus|hover|last-(?:child|of-type)|link|only-(?:child|of-type)|root|:selection|target|visited)/gi;
    content = content.replace(pattern, function (ignore, f1) {
        return ':' + f1.toLowerCase();
    });

    // if there is a @charset, then only allow one, and push to the top of the file.
    content = content.replace(/^(.*)(@charset \"[^\"]*\";)/g, "$2$1");
    content = content.replace(/^(\s*@charset [^;]+;\s*)+/g, "$1");

    // lowercase some more common functions
    pattern = /:(lang|not|nth-child|nth-last-child|nth-last-of-type|nth-of-type|(?:-(?:atsc|khtml|moz|ms|o|wap|webkit)-)?any)\(/gi;
    content = content.replace(pattern, function (ignore, f1) {
        return ':' + f1.toLowerCase() + '(';
    });

    // lower case some common function that can be values
    // NOTE: rgb() isn't useful as we replace with #hex later, as well as and() is already done for us right after this
    pattern = /([:,\( ]\s*)(attr|color-stop|from|rgba|to|url|(?:-(?:atsc|khtml|moz|ms|o|wap|webkit)-)?(?:calc|max|min|(?:repeating-)?(?:linear|radial)-gradient)|-webkit-gradient)/gi;
    content = content.replace(pattern, function (ignore, f1, f2) {
        return f1 + f2.toLowerCase();
    });

    // put the space back in some cases, to support stuff like
    // @media screen and (-webkit-min-device-pixel-ratio:0){
    content = content.replace(/\band\(/gi, "and (");

    // remove the spaces after the things that should not have spaces after them.
    content = content.replace(/([!{}:;>+\(\[,])\s+/g, "$1");

    // remove unnecessary semicolons
    content = content.replace(/;+\}/g, "}");

    // replace 0(px,em,%) with 0.
    content = content.replace(/(^|[^.0-9\\])(?:0?\.)?0(?:ex|ch|r?em|vw|vh|vmin|vmax|cm|mm|in|pt|pc|px|deg|g?rad|turn|m?s|k?Hz|dpi|dpcm|dppx|%)/gi, "$10");

    // Replace x.0(px,em,%) with x(px,em,%).
    content = content.replace(/([0-9])\.0(ex|ch|r?em|vw|vh|vmin|vmax|cm|mm|in|pt|pc|px|deg|g?rad|turn|m?s|k?Hz|dpi|dpcm|dppx|%| |;)/gi, "$1$2");

    // replace 0 0 0 0; with 0.
    content = content.replace(/:0 0 0 0(;|\})/g, ":0$1");
    content = content.replace(/:0 0 0(;|\})/g, ":0$1");
    content = content.replace(/:0 0(;|\})/g, ":0$1");

    // replace background-position:0; with background-position:0 0;
    // same for transform-origin and box-shadow
    pattern = /(background-position|transform-origin|webkit-transform-origin|moz-transform-origin|o-transform-origin|ms-transform-origin|box-shadow):0(;|\})/gi;
    content = content.replace(pattern, function (ignore, f1, f2) {
        return f1.toLowerCase() + ":0 0" + f2;
    });

    // replace 0.6 to .6, but only when preceded by : or a white-space
    content = content.replace(/(:|\s)0+\.(\d+)/g, "$1.$2");

    // shorten colors from rgb(51,102,153) to #336699
    // this makes it more likely that it'll get further compressed in the next step.
    pattern = /rgb\s*\(\s*([0-9,\s]+)\s*\)/gi;
    content = content.replace(pattern, function (ignore, f1) {
        rgbcolors = f1.split(",");
        hexcolor = "#";
        for (i = 0; i < rgbcolors.length; i += 1) {
            val = parseInt(rgbcolors[i], 10);
            if (val < 16) {
                hexcolor += "0";
            }
            if (val > 255) {
                val = 255;
            }
            hexcolor += val.toString(16);
        }
        return hexcolor;
    });

    // Shorten colors from #AABBCC to #ABC.
    content = compressHexColors(content);

    // Replace #f00 -> red
    content = content.replace(/(:|\s)(#f00)(;|})/g, "$1red$3");

    // Replace other short color keywords
    content = content.replace(/(:|\s)(#000080)(;|})/g, "$1navy$3");
    content = content.replace(/(:|\s)(#808080)(;|})/g, "$1gray$3");
    content = content.replace(/(:|\s)(#808000)(;|})/g, "$1olive$3");
    content = content.replace(/(:|\s)(#800080)(;|})/g, "$1purple$3");
    content = content.replace(/(:|\s)(#c0c0c0)(;|})/g, "$1silver$3");
    content = content.replace(/(:|\s)(#008080)(;|})/g, "$1teal$3");
    content = content.replace(/(:|\s)(#ffa500)(;|})/g, "$1orange$3");
    content = content.replace(/(:|\s)(#800000)(;|})/g, "$1maroon$3");

    // border: none -> border:0
    pattern = /(border|border-top|border-right|border-bottom|border-left|outline|background):none(;|\})/gi;
    content = content.replace(pattern, function (ignore, f1, f2) {
        return f1.toLowerCase() + ":0" + f2;
    });

    // shorter opacity IE filter
    content = content.replace(/progid:DXImageTransform\.Microsoft\.Alpha\(Opacity=/gi, "alpha(opacity=");

    // Find a fraction that is used for Opera's -o-device-pixel-ratio query
    // Add token to add the "\" back in later
    content = content.replace(/\(([\-A-Za-z]+):([0-9]+)\/([0-9]+)\)/g, "($1:$2___QUERY_FRACTION___$3)");

    // remove empty rules.
    content = content.replace(/[^\};\{\/]+\{\}/g, "");

    // Add "\" back to fix Opera -o-device-pixel-ratio query
    content = content.replace(/___QUERY_FRACTION___/g, "/");

    // some source control tools don't like it when files containing lines longer
    // than, say 8000 characters, are checked in. The linebreak option is used in
    // that case to split long lines after a specific column.
    if (options.maxLineLen > 0) {
        for (i = 0, len = content.length; i < len; i += 1) {
            c = content.charAt(i);
            line.push(c);
            if (c === '}' && line.length > options.maxLineLen) {
                lines.push(line.join(''));
                line = [];
            }
        }
        if (line.length) {
            lines.push(line.join(''));
        }

        content = lines.join('\n');
    }

    // replace multiple semi-colons in a row by a single one
    // see SF bug #1980989
    content = content.replace(/;;+/g, ";");

    // trim the final string (for any leading or trailing white spaces)
    content = content.replace(/(^\s*|\s*$)/g, "");

    // restore preserved tokens
    for (i = preservedTokens.length - 1; i >= 0 ; i--) {
        content = content.replace(___PRESERVED_TOKEN_ + i + "___", preservedTokens[i], "g");
    }

    // restore preserved newlines
    content = content.replace(/___PRESERVED_NEWLINE___/g, '\n');

    // return
    return content;
}

// Uglify CSS files

function processFiles(filenames, options) {

    var nFiles = filenames.length,
        uglies = [],
        index,
        filename,
        content;

    options = options || defaultOptions;

    if (options.convertUrls) {
        options.target = path.resolve(process.cwd(), options.convertUrls).split(PATH_SEP);
    }

    // process files
    for (index = 0; index < nFiles; index += 1) {
        filename = filenames[index];
        try {
            content = fs.readFileSync(filename, 'utf8');
            if (content.length) {
                if (options.convertUrls) {
                    options.source = path.resolve(process.cwd(), filename).split(PATH_SEP);
                    options.source.pop();
                }
                uglies.push(processString(content, options));
            }
        } catch (e) {
            if (options.debug) {
                console.error('uglifycss: unable to process "' + filename + '"\n' + e.stack);
            } else {
                console.error('uglifycss: unable to process "' + filename + '"\n\t' + e);
            }
            process.exit(1);
        }
    }

    // return concat'd results
    return uglies.join('');
}

module.exports = {
    defaultOptions: defaultOptions,
    processString: processString,
    processFiles: processFiles
};

}).call(this,require('_process'))
},{"_process":3,"fs":1,"path":2}]},{},[4])(4)
});