class MiyomuDemoSource extends ComicSource {
    name = "Miyomu Demo";
    key = "miyomu_demo";
    version = "1.0.0";
    minAppVersion = "0.1.0";
    url = "";

    search = {
        load: async (keyword, options, page) => {
            const response = await Network.get(
                __BASE_URL + "/search?q=" + encodeURIComponent(keyword)
            );
            if (response.status !== 200) {
                throw new Error("search HTTP " + response.status);
            }
            const document = new HtmlDocument(response.body);
            try {
                const comics = document.querySelectorAll("a.comic").map(link => {
                    const author = link.querySelector(".author");
                    return new Comic({
                        id: link.attributes["data-id"],
                        title: link.querySelector(".title").text,
                        subtitle: author ? author.text : "",
                        cover: ""
                    });
                });
                return { comics, maxPage: 1 };
            } finally {
                document.dispose();
            }
        }
    };

    comic = {
        loadInfo: async id => {
            const response = await Network.get(__BASE_URL + "/comic/" + encodeURIComponent(id));
            if (response.status !== 200) {
                throw new Error("details HTTP " + response.status);
            }
            const document = new HtmlDocument(response.body);
            try {
                const author = document.querySelector(".author");
                const chapters = {};
                document.querySelectorAll(".chapters a").forEach(link => {
                    chapters[link.attributes["data-id"]] = link.text;
                });
                return new ComicDetails({
                    title: document.querySelector("h1.title").text,
                    cover: "",
                    description: "A local fixture served by Miyomu's demo server.",
                    tags: { author: author ? [author.text] : [] },
                    chapters,
                    updateTime: "2026-10-28"
                });
            } finally {
                document.dispose();
            }
        },

        loadEp: async (comicId, epId) => {
            const response = await Network.get(__BASE_URL + "/chapter/" + encodeURIComponent(epId));
            if (response.status !== 200) {
                throw new Error("pages HTTP " + response.status);
            }
            const document = new HtmlDocument(response.body);
            try {
                const images = document.querySelectorAll("img[data-src]")
                    .map(image => __BASE_URL + image.attributes["data-src"]);
                return { images };
            } finally {
                document.dispose();
            }
        }
    };
}
