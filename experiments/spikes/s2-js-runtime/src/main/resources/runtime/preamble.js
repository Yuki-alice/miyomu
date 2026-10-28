// Minimal host shim for the S2 spike. This is our own implementation of a
// Venera-shaped read-only contract; it is deliberately not copied from Venera.

globalThis.ComicSource = class ComicSource {
    static sources = {};

    constructor() {
        if (this.key) {
            ComicSource.sources[this.key] = this;
        }
    }

    loadData(key) {
        return __hostKvGet(this.key + ":" + key);
    }

    saveData(key, value) {
        __hostKvSet(this.key + ":" + key, String(value));
    }
};

globalThis.Comic = class Comic {
    constructor(values) {
        Object.assign(this, values);
    }
};

globalThis.ComicDetails = class ComicDetails {
    constructor(values) {
        Object.assign(this, values);
    }
};

globalThis.Network = {
    async get(url, headers) {
        const body = await __hostHttpGet(url, JSON.stringify(headers || {}));
        return { status: 200, headers: {}, body };
    }
};

globalThis.Cookie = class Cookie {
    constructor(values) {
        this.name = values.name;
        this.value = values.value;
        this.domain = values.domain || null;
    }
};

Network.setCookies = function (url, cookies) {
    (cookies || []).forEach(cookie => {
        __hostSetCookie(url, cookie.name, cookie.value, cookie.domain || "");
    });
};

Network.getCookies = function (url) {
    return JSON.parse(__hostGetCookies(url));
};

Network.deleteCookies = function (url) {
    return __hostDeleteCookies(url);
};

function __hostIds(value) {
    return value ? value.split(",").filter(Boolean).map(Number) : [];
}

globalThis.HtmlDocument = class HtmlDocument {
    constructor(html) {
        this._id = __hostParseHtml(html);
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
        const elementId = __hostDocGetElementById(this._id, id);
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

globalThis.__invoke = async function (action, payloadJson) {
    const payload = JSON.parse(payloadJson || "{}");
    const source = payload.sourceKey
        ? ComicSource.sources[payload.sourceKey]
        : Object.values(ComicSource.sources)[0];
    if (!source) {
        throw new Error("no source registered: " + (payload.sourceKey || "any"));
    }
    switch (action) {
        case "search":
            return JSON.stringify(await source.search.load(payload.keyword, {}, 1));
        case "details":
            return JSON.stringify(await source.comic.loadInfo(payload.id));
        case "pages":
            return JSON.stringify(await source.comic.loadEp(payload.comicId, payload.epId));
        case "kv":
            source.saveData("round", String(payload.value));
            return source.loadData("round");
        default:
            throw new Error("unknown action: " + action);
    }
};
