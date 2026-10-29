// OmoFun (omofun.in) — MacCMS front-end; play streams come from the /​_dyn_plays JSON endpoint.
// Verified 2026-10-30 against the live site (search / detail / _dyn_plays), ADR-0014 D4.
const CONFIG = {
    key: "omofun",
    name: "OmoFun",
    version: "1.0.0",
    baseUrl: "https://omofun.in",
    headers: {
        Referer: "https://omofun.in/",
        "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
    }
};

class OmofunSource extends AnimeSource {
    name = CONFIG.name;
    key = CONFIG.key;
    version = CONFIG.version;
    url = CONFIG.baseUrl;

    async search(query, page) {
        const url = CONFIG.baseUrl + "/vod/search.html?wd=" + encodeURIComponent(query) +
            (page > 1 ? "&page=" + page : "");
        const response = await Network.get(url, CONFIG.headers);
        const doc = new HtmlDocument(response.body);
        const list = [];
        for (const item of doc.querySelectorAll(".module-card-item")) {
            const link = item.querySelector(".module-card-item-title a");
            const poster = item.querySelector(".module-card-item-poster");
            const coverImg = item.querySelector("img");
            const id = poster ? poster.attributes["href"] : (link ? link.attributes["href"] : null);
            if (!id) continue;
            list.push({
                id: id,
                title: link ? link.text.trim() : "",
                cover: coverImg
                    ? Std.resolveUrl(CONFIG.baseUrl, coverImg.attributes["data-original"] || coverImg.attributes["src"])
                    : null,
                subtitle: (item.querySelector(".module-item-note") || {}).text || null
            });
        }
        doc.dispose();
        return { list: list, maxPage: null };
    }

    async detail(detailPath) {
        const response = await Network.get(Std.resolveUrl(CONFIG.baseUrl, detailPath), CONFIG.headers);
        const doc = new HtmlDocument(response.body);

        const titleEl = doc.querySelector(".module-info-heading h1");
        const title = titleEl ? titleEl.text.trim() : detailPath;
        const infoImg = doc.querySelector(".module-info-pic img") || doc.querySelector(".module-info-main img");
        const cover = infoImg
            ? Std.resolveUrl(CONFIG.baseUrl, infoImg.attributes["data-original"] || infoImg.attributes["src"])
            : null;
        const descEl = doc.querySelector(".module-info-introduction-content") ||
            doc.querySelector(".module-info-introduction");
        const description = descEl ? descEl.text.trim() : null;

        const tags = { type: [] };
        for (const a of doc.querySelectorAll(".module-info-tag-link a")) {
            if (a.text.trim()) tags.type.push(a.text.trim());
        }

        // Each .module-play-list-content is one play route (线路).
        const episodes = [];
        let line = 0;
        for (const group of doc.querySelectorAll(".module-play-list-content")) {
            for (const anchor of group.querySelectorAll("a.module-play-list-link")) {
                const href = anchor.attributes["href"] || "";
                if (!href) continue;
                const name = (anchor.querySelector("span") || anchor).text.trim();
                episodes.push({
                    key: href,
                    name: name,
                    url: href,
                    line: line,
                    number: Std.parseEpisodeNumber(name)
                });
            }
            line += 1;
        }
        doc.dispose();
        return { title: title, cover: cover, description: description, tags: tags, episodes: episodes };
    }

    async resolve(episodeUrl) {
        // /vod/play/<id>/epN.html -> /_dyn_plays/<id>/epN  (JSON with one entry per line).
        const path = String(episodeUrl).replace(/^.*\/vod\/play\//, "").replace(/\.html$/, "");
        const response = await Network.get(
            CONFIG.baseUrl + "/_dyn_plays/" + path,
            Object.assign({}, CONFIG.headers, { "X-Requested-With": "XMLHttpRequest" })
        );
        const payload = JSON.parse(response.body);
        const streams = (payload.video_plays || []).map(entry => {
            const url = String(entry.play_data || "");
            return {
                url: url,
                quality: entry.src_site || null,
                type: /\.m3u8($|\?)/i.test(url) ? "hls" : "mp4"
            };
        }).filter(stream => stream.url);
        if (streams.length === 0) {
            throw new Error("omofun: no streams for " + episodeUrl);
        }
        return { urls: streams, headers: CONFIG.headers };
    }
}
