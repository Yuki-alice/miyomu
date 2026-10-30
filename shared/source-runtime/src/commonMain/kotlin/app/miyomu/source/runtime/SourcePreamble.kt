package app.miyomu.source.runtime

internal const val SourcePreamble: String = """
globalThis.MiyomuSourceBase = class MiyomuSourceBase {
    loadData(key) {
        const value = __hostKvGet(this.key + ":" + key);
        return value === undefined ? null : value;
    }

    saveData(key, value) {
        __hostKvSet(this.key + ":" + key, value == null ? "" : String(value));
    }

    deleteData(key) {
        __hostKvDelete(this.key + ":" + key);
    }

    loadSetting(key) {
        const value = this.loadData("setting:" + key);
        if (value == null) return null;
        try {
            return JSON.parse(value);
        } catch (error) {
            return value;
        }
    }

    translate(key) {
        return key;
    }
};

globalThis.ComicSource = class ComicSource extends MiyomuSourceBase {
    static sources = Object.create(null);
};

globalThis.AnimeSource = class AnimeSource extends MiyomuSourceBase {
    static sources = ComicSource.sources;
};

globalThis.Comic = class Comic {
    constructor(values) {
        Object.assign(this, values || {});
    }
};

globalThis.ComicDetails = class ComicDetails {
    constructor(values) {
        Object.assign(this, values || {});
    }
};

globalThis.Cookie = class Cookie {
    constructor(values) {
        values = values || {};
        this.name = values.name;
        this.value = values.value;
        this.domain = values.domain || null;
    }
};

// Venera's diagnostic surface; routed to the host log so source logging is visible.
globalThis.console = {
    log(...args) { __hostLog("log", Array.from(args).join(" ")); },
    info(...args) { __hostLog("info", Array.from(args).join(" ")); },
    warn(...args) { __hostLog("warn", Array.from(args).join(" ")); },
    error(...args) { __hostLog("error", Array.from(args).join(" ")); },
    debug(...args) { __hostLog("debug", Array.from(args).join(" ")); },
};

// App metadata. `platform` is deliberately absent: the preamble is shared by the JVM and
// Android targets, and reporting the wrong platform would silently change source branches.
// Sources that read it are reported by tools/venera_compat.py instead of getting a lie.
globalThis.APP = {
    get version() { return "0.1.0"; },
    get locale() { return "zh_CN"; },
};

// Comment model: sources construct it when rendering a comments page. Display is not
// implemented yet, so the object only needs to hold its fields.
globalThis.Comment = function Comment(values) {
    values = values || {};
    this.userName = values.userName;
    this.avatar = values.avatar;
    this.content = values.content;
    this.time = values.time;
    this.replyCount = values.replyCount;
    this.id = values.id;
    this.isLiked = values.isLiked;
    this.score = values.score;
    this.voteStatus = values.voteStatus;
};

// Image handle. Real decoding/transform is host work that sources only reach through
// Venera's image pipeline; an empty handle keeps signatures satisfied.
globalThis.Image = class Image {
    constructor(key) {
        this.key = key == null ? 0 : key;
    }

    static get empty() {
        return new Image(0);
    }
};

globalThis.Network = {
    async sendRequest(method, url, headers, data) {
        const responseJson = await __hostHttpRequest(
            method,
            url,
            JSON.stringify(headers || {}),
            data == null ? null : String(data)
        );
        return JSON.parse(responseJson);
    },

    get(url, headers) {
        return this.sendRequest("GET", url, headers, null);
    },

    post(url, headers, data) {
        return this.sendRequest("POST", url, headers, data);
    },

    put(url, headers, data) {
        return this.sendRequest("PUT", url, headers, data);
    },

    patch(url, headers, data) {
        return this.sendRequest("PATCH", url, headers, data);
    },

    delete(url, headers) {
        return this.sendRequest("DELETE", url, headers, null);
    },

    setCookies() {
        throw new Error("Network.setCookies is not supported in this runtime yet");
    },

    getCookies() {
        throw new Error("Network.getCookies is not supported in this runtime yet");
    },

    deleteCookies() {
        throw new Error("Network.deleteCookies is not supported in this runtime yet");
    }
};

// Web-fetch shaped adapter over Network: sources written for Venera expect a `Response`-like
// object. Binary paths (`arrayBuffer`) stay unsupported until Network.fetchBytes exists, and
// say so instead of returning garbage.
globalThis.fetch = async function (url, options) {
    options = options || {};
    const response = await Network.sendRequest(
        options.method || "GET",
        url,
        options.headers || {},
        options.body == null ? null : options.body
    );
    const text = async () => response.body;
    return {
        ok: response.status >= 200 && response.status < 300,
        status: response.status,
        statusText: "",
        headers: response.headers,
        text: text,
        json: async () => JSON.parse(await text()),
        arrayBuffer: async () => {
            throw new Error("fetch(...).arrayBuffer() needs Network.fetchBytes, not implemented");
        }
    };
};

// Small helpers Venera exposes as globals (init.js): random values and uuids.
globalThis.randomInt = function (min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
};

globalThis.randomDouble = function (min, max) {
    return Math.random() * (max - min) + min;
};

globalThis.createUuid = function () {
    let uuid = "";
    for (let i = 0; i < 36; i++) {
        if (i === 8 || i === 13 || i === 18 || i === 23) {
            uuid += "-";
        } else if (i === 14) {
            uuid += "4";
        } else if (i === 19) {
            uuid += (Math.floor(Math.random() * 4) + 8).toString(16);
        } else {
            uuid += Math.floor(Math.random() * 16).toString(16);
        }
    }
    return uuid;
};

globalThis.log = function (...args) {
    __hostLog("log", Array.from(args).join(" "));
};

// Venera's encoding/crypto helpers. Everything runs on the host (java.security / javax.crypto)
// so binary data never crosses the JS boundary twice; `__bytesOf` normalises whatever a source
// hands us (string, Uint8Array, ArrayBuffer, view) into something the host can read.
function __bytesOf(value) {
    if (value == null) return new Uint8Array(0);
    if (typeof value === "string") return value;
    if (value instanceof ArrayBuffer) return new Uint8Array(value);
    if (ArrayBuffer.isView(value)) {
        return new Uint8Array(value.buffer, value.byteOffset, value.byteLength);
    }
    return new Uint8Array(0);
}

// Everything the host returns for a binary result is handed back as a real ArrayBuffer, which
// is what Venera's own globals return and what `instanceof` checks in sources expect.
function __toArrayBuffer(value) {
    if (value instanceof ArrayBuffer) return value;
    if (ArrayBuffer.isView(value)) {
        return value.buffer.slice(value.byteOffset, value.byteOffset + value.byteLength);
    }
    return new ArrayBuffer(0);
}

globalThis.Convert = {
    encodeUtf8: (str) => __toArrayBuffer(__hostConvertBytes("utf8:encode", str, null, null)),
    decodeUtf8: (value) => __hostConvertText("utf8:decode", __bytesOf(value), null, null),
    encodeGbk: (str) => __toArrayBuffer(__hostConvertBytes("gbk:encode", str, null, null)),
    decodeGbk: (value) => __hostConvertText("gbk:decode", __bytesOf(value), null, null),
    encodeBase64: (value) => __hostConvertText("base64:encode", __bytesOf(value), null, null),
    decodeBase64: (value) => __toArrayBuffer(__hostConvertBytes("base64:decode", String(value), null, null)),
    hexEncode: (value) => __hostConvertText("hex:encode", __bytesOf(value), null, null),
    // Digests return the raw ArrayBuffer, matching Venera: sources hex-encode them explicitly
    // (`Convert.hexEncode(Convert.md5(...))`).
    md5: (value) => __toArrayBuffer(__hostConvertBytes("digest-raw:md5", __bytesOf(value), null, null)),
    sha1: (value) => __toArrayBuffer(__hostConvertBytes("digest-raw:sha1", __bytesOf(value), null, null)),
    sha256: (value) => __toArrayBuffer(__hostConvertBytes("digest-raw:sha256", __bytesOf(value), null, null)),
    sha512: (value) => __toArrayBuffer(__hostConvertBytes("digest-raw:sha512", __bytesOf(value), null, null)),
    hmac: (key, value, hash) =>
        __toArrayBuffer(__hostConvertBytes("hmac:" + (hash || "sha256"), __bytesOf(value), __bytesOf(key), null)),
    hmacString: (key, value, hash) =>
        __hostConvertText("hmac-string:" + (hash || "sha256"), __bytesOf(value), __bytesOf(key), null),
    encryptAesEcb: (value, key) =>
        __toArrayBuffer(__hostConvertBytes("aes-ecb:enc", __bytesOf(value), __bytesOf(key), null)),
    decryptAesEcb: (value, key) =>
        __toArrayBuffer(__hostConvertBytes("aes-ecb:dec", __bytesOf(value), __bytesOf(key), null)),
    encryptAesCbc: (value, key, iv) =>
        __toArrayBuffer(__hostConvertBytes("aes-cbc:enc", __bytesOf(value), __bytesOf(key), __bytesOf(iv))),
    decryptAesCbc: (value, key, iv) =>
        __toArrayBuffer(__hostConvertBytes("aes-cbc:dec", __bytesOf(value), __bytesOf(key), __bytesOf(iv))),
    encryptAesCfb: (value, key, iv) =>
        __toArrayBuffer(__hostConvertBytes("aes-cfb:enc", __bytesOf(value), __bytesOf(key), __bytesOf(iv))),
    decryptAesCfb: (value, key, iv) =>
        __toArrayBuffer(__hostConvertBytes("aes-cfb:dec", __bytesOf(value), __bytesOf(key), __bytesOf(iv))),
    encryptAesOfb: (value, key, iv) =>
        __toArrayBuffer(__hostConvertBytes("aes-ofb:enc", __bytesOf(value), __bytesOf(key), __bytesOf(iv))),
    decryptAesOfb: (value, key, iv) =>
        __toArrayBuffer(__hostConvertBytes("aes-ofb:dec", __bytesOf(value), __bytesOf(key), __bytesOf(iv))),
    decryptRsa: () => {
        throw new Error("Convert.decryptRsa is not supported in this runtime yet");
    },
};

globalThis.UI = {
    // Headless runtime: a toast/dialog is a side effect we cannot honour, but throwing would
    // abort the source mid-flow (sources do call these for user feedback). Log instead.
    showMessage(message) {
        __hostLog("ui", String(message));
    },
    showDialog(title, content) {
        __hostLog("ui", String(title) + ": " + String(content));
    }
};

function __hostIds(value) {
    return value ? value.split(",").filter(Boolean).map(Number) : [];
}

globalThis.HtmlDocument = class HtmlDocument {
    constructor(html) {
        this._id = __hostParseHtml(String(html));
        this._disposed = false;
    }

    _ensure() {
        if (this._disposed) {
            throw new Error("document disposed");
        }
    }

    querySelector(selector) {
        this._ensure();
        const id = __hostDocQuery(this._id, selector);
        return id == null ? null : new HtmlElement(id, this);
    }

    querySelectorAll(selector) {
        this._ensure();
        return __hostIds(__hostDocQueryAll(this._id, selector))
            .map(id => new HtmlElement(id, this));
    }

    getElementById(id) {
        this._ensure();
        const elementId = __hostDocGetElementById(this._id, String(id));
        return elementId == null ? null : new HtmlElement(elementId, this);
    }

    dispose() {
        if (!this._disposed) {
            __hostDisposeDoc(this._id);
            this._disposed = true;
        }
    }
};

globalThis.HtmlElement = class HtmlElement {
    constructor(id, document) {
        this._id = id;
        this._document = document;
    }

    _ensure() {
        this._document._ensure();
    }

    querySelector(selector) {
        this._ensure();
        const id = __hostElementQuery(this._id, selector);
        return id == null ? null : new HtmlElement(id, this._document);
    }

    querySelectorAll(selector) {
        this._ensure();
        return __hostIds(__hostElementQueryAll(this._id, selector))
            .map(id => new HtmlElement(id, this._document));
    }

    get text() {
        this._ensure();
        return __hostElementText(this._id);
    }

    get innerHTML() {
        this._ensure();
        return __hostElementHtml(this._id);
    }

    get attributes() {
        this._ensure();
        return JSON.parse(__hostElementAttributes(this._id));
    }

    get classNames() {
        this._ensure();
        const value = __hostElementClassNames(this._id);
        return value ? value.split(/\s+/).filter(Boolean) : [];
    }

    get id() {
        this._ensure();
        return __hostElementId(this._id);
    }

    get localName() {
        this._ensure();
        return __hostElementLocalName(this._id);
    }

    get children() {
        this._ensure();
        return __hostIds(__hostElementChildren(this._id))
            .map(id => new HtmlElement(id, this._document));
    }

    get nodes() {
        return this.children;
    }

    get parent() {
        this._ensure();
        const id = __hostElementParent(this._id);
        return id == null ? null : new HtmlElement(id, this._document);
    }

    get previousElementSibling() {
        this._ensure();
        const id = __hostElementPreviousSibling(this._id);
        return id == null ? null : new HtmlElement(id, this._document);
    }

    get nextElementSibling() {
        this._ensure();
        const id = __hostElementNextSibling(this._id);
        return id == null ? null : new HtmlElement(id, this._document);
    }

    toElement() {
        return this;
    }
};

globalThis.Std = {
    resolveUrl(base, url) {
        if (!url) return url;
        url = String(url);
        if (/^https?:\/\//i.test(url)) return url;
        if (url.startsWith("//")) {
            const scheme = /^https:/i.test(String(base)) ? "https:" : "http:";
            return scheme + url;
        }
        return String(base).replace(/\/+$/, "") + "/" + url.replace(/^\/+/, "");
    },

    // Absolute/relative m3u8 urls embedded in player scripts, with escaped slashes unquoted.
    extractM3u8(html) {
        const text = String(html).replace(/\\\//g, "/");
        const found = [];
        const re = /https?:\/\/[^\s"'<>\\]+?\.m3u8/gi;
        let match;
        while ((match = re.exec(text)) !== null) {
            if (found.indexOf(match[0]) < 0) found.push(match[0]);
        }
        return found;
    },

    // AppleCMS vod_play_url: groups joined by "${'$'}${'$'}${'$'}", each "name${'$'}url#name${'$'}url".
    parseAppleCmsPlay(vodPlayFrom, vodPlayUrl) {
        const froms = String(vodPlayFrom || "").split("${'$'}${'$'}${'$'}");
        const groups = String(vodPlayUrl || "").split("${'$'}${'$'}${'$'}");
        const lines = [];
        for (let i = 0; i < froms.length; i++) {
            const episodes = (groups[i] || "").split("#").map(item => {
                const index = item.indexOf("$");
                if (index < 0) return null;
                const name = item.slice(0, index).trim();
                const url = item.slice(index + 1).trim();
                if (!url) return null;
                return { name: name, url: url };
            }).filter(Boolean);
            if (episodes.length > 0) lines.push({ from: froms[i].trim(), episodes: episodes });
        }
        return lines;
    },

    parseEpisodeNumber(name) {
        const text = String(name || "");
        const patterns = [
            /第\s*([0-9]+)\s*[集话期回]/,
            /\b(?:S\d+\s*E|E)([0-9]+)\b/i,
            /\b([0-9]{1,4})\b/
        ];
        for (const pattern of patterns) {
            const match = pattern.exec(text);
            if (match) {
                const value = parseFloat(match[1]);
                if (!isNaN(value)) return value;
            }
        }
        return null;
    }
};

globalThis.__miyomuCreateSource = async function (source) {
    if (source == null || typeof source !== "object") {
        throw new Error("source instance is required");
    }
    if (typeof source.init === "function") {
        await source.init();
    }
    const key = source.key;
    if (typeof key !== "string" || !/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(key)) {
        throw new Error("source key is missing or invalid");
    }
    if (key === "__proto__" || key === "constructor" || key === "prototype") {
        throw new Error("source key is reserved: " + key);
    }
    if (typeof source.name !== "string" || source.name.length === 0) {
        throw new Error("source name is required");
    }
    if (typeof source.version !== "string" || source.version.length === 0) {
        throw new Error("source version is required");
    }
    ComicSource.sources[key] = source;
    return JSON.stringify({
        key: key,
        name: source.name,
        version: source.version,
        minAppVersion: source.minAppVersion || null,
        url: source.url || null
    });
};

globalThis.__miyomuInvoke = async function (action, sourceKey, payloadJson) {
    const source = ComicSource.sources[sourceKey];
    if (!source) {
        throw new Error("source not loaded: " + sourceKey);
    }
    const payload = JSON.parse(payloadJson || "{}");

    if (action === "search") {
        if (!source.search || typeof source.search.load !== "function") {
            throw new Error("source does not support search");
        }
        return JSON.stringify(await source.search.load(payload.query, [], payload.page || 1));
    }

    if (action === "details") {
        if (!source.comic || typeof source.comic.loadInfo !== "function") {
            throw new Error("source does not support comic details");
        }
        return JSON.stringify(await source.comic.loadInfo(payload.id));
    }

    if (action === "pages") {
        if (!source.comic || typeof source.comic.loadEp !== "function") {
            throw new Error("source does not support chapter pages");
        }
        return JSON.stringify(await source.comic.loadEp(payload.comicId, payload.chapterId == null ? null : payload.chapterId));
    }

    if (action === "animeSearch") {
        if (typeof source.search !== "function") {
            throw new Error("source does not support search");
        }
        return JSON.stringify(await source.search(payload.query, payload.page || 1));
    }

    if (action === "animeDetail") {
        if (typeof source.detail !== "function") {
            throw new Error("source does not support detail");
        }
        return JSON.stringify(await source.detail(payload.id));
    }

    if (action === "animeResolve") {
        if (typeof source.resolve !== "function") {
            throw new Error("source does not support resolve");
        }
        return JSON.stringify(await source.resolve(payload.episodeUrl));
    }

    throw new Error("unknown action: " + action);
};
"""
