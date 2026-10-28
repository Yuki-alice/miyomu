package app.miyomu.source.runtime

internal const val SourcePreamble: String = """
globalThis.ComicSource = class ComicSource {
    static sources = Object.create(null);

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

globalThis.fetch = async function () {
    throw new Error("fetch is not supported; use Network.get");
};

globalThis.UI = {
    showMessage() {
        throw new Error("UI is not available in the headless source runtime");
    },
    showDialog() {
        throw new Error("UI is not available in the headless source runtime");
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

    throw new Error("unknown action: " + action);
};
"""
