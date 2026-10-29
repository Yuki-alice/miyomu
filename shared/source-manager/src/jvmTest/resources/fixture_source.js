class FixtureSource extends ComicSource {
    name = "Fixture Source";
    key = "fixture";
    version = "1.0.0";
    url = "";

    search = {
        load: async (keyword, options, page) => ({
            comics: [new Comic({ id: "c1", title: "Result " + keyword, cover: "" })],
            maxPage: 1
        })
    };

    comic = {
        loadInfo: async (id) => new ComicDetails({
            title: "Fixture Comic",
            cover: "",
            description: "Fixture description",
            tags: { author: ["Fixture Author"] },
            chapters: { ch1: "Chapter 1", ch2: "Chapter 2" }
        }),
        loadEp: async (comicId, epId) => ({
            images: ["https://example.test/1.jpg", "https://example.test/2.jpg"]
        })
    };
}
