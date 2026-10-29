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
