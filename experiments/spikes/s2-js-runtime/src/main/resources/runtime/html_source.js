// HTML fixture source for S2: exercises HtmlDocument + CSS selectors + cookies.
class HtmlDemoSource extends ComicSource {
    name = "HTML Demo";
    key = "html_demo";
    version = "1.0.0";
    url = "";

    search = {
        load: async (keyword, options, page) => {
            const response = await Network.get(
                __BASE_URL + "/html/search?q=" + encodeURIComponent(keyword)
            );
            const document = new HtmlDocument(response.body);
            try {
                const comics = document.querySelectorAll("a.comic").map(link => {
                    const author = link.querySelector(".author");
                    return new Comic({
                        id: link.attributes["data-id"],
                        title: link.querySelector(".title").text,
                        author: author ? author.text : "",
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
        loadInfo: async (id) => {
            const response = await Network.get(__BASE_URL + "/html/comic/" + id);
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
                    tags: { author: author ? [author.text] : [] },
                    chapters,
                    updateTime: "2026-10-28"
                });
            } finally {
                document.dispose();
            }
        },
        loadEp: async (comicId, epId) => {
            const response = await Network.get(__BASE_URL + "/html/chapter/" + epId);
            const document = new HtmlDocument(response.body);
            try {
                const images = document.querySelectorAll("img[data-src]").map(image => {
                    return __BASE_URL + image.attributes["data-src"];
                });
                return { images };
            } finally {
                document.dispose();
            }
        }
    };
}

const htmlDemoSource = new HtmlDemoSource();
ComicSource.sources[htmlDemoSource.key] = htmlDemoSource;
