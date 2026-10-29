class DemoAnimeSource extends AnimeSource {
    name = "Demo Anime";
    key = "demo_anime";
    version = "1.0.0";
    url = "";

    async search(query, page) {
        const html = await Network.get(__BASE_URL + "/anime/search?q=" + encodeURIComponent(query) + "&page=" + page);
        const doc = new HtmlDocument(html.body);
        const list = [];
        for (const card of doc.querySelectorAll(".anime-item")) {
            const id = card.attributes["data-id"] || "";
            const title = card.querySelector(".title")?.text?.trim() || id;
            const cover = card.querySelector("img")?.attributes["data-src"] || null;
            list.push({ id: id, title: title, cover: cover });
        }
        doc.dispose();
        return { list: list, maxPage: 1 };
    }

    async detail(id) {
        const html = await Network.get(__BASE_URL + "/anime/detail?id=" + encodeURIComponent(id));
        const doc = new HtmlDocument(html.body);
        const title = doc.querySelector(".title")?.text?.trim() || id;
        const cover = doc.querySelector("img")?.attributes["data-src"] || null;
        const description = doc.querySelector(".desc")?.text?.trim() || "";
        const episodes = [];
        let line = 0;
        for (const group of doc.querySelectorAll(".line")) {
            for (const anchor of group.querySelectorAll("a")) {
                const url = anchor.attributes["data-url"] || "";
                const name = anchor.text.trim();
                const number = parseFloat(anchor.attributes["data-number"] || "");
                episodes.push({
                    key: url,
                    name: name,
                    url: url,
                    line: line,
                    number: isNaN(number) ? null : number
                });
            }
            line += 1;
        }
        doc.dispose();
        return { title: title, cover: cover, description: description, episodes: episodes };
    }

    async resolve(episodeUrl) {
        const response = await Network.get(
            __BASE_URL + "/api/resolve?ep=" + encodeURIComponent(episodeUrl),
            { "X-Miyomu-Test": "resolve" }
        );
        const payload = JSON.parse(response.body);
        return {
            urls: payload.urls,
            headers: { Referer: __BASE_URL + "/", "User-Agent": "MiyomuAnimeTest/1.0" }
        };
    }
}
