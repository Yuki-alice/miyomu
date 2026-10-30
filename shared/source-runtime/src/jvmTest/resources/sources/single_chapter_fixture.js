class SingleChapterFixtureSource extends ComicSource {
    name = "Single Chapter Fixture";
    key = "single_chapter_fixture";
    version = "1.0.0";
    minAppVersion = "0.1.0";
    url = "";

    search = {
        load: async () => ({
            comics: [new Comic({ id: "g1", title: "Single Gallery" })],
            maxPage: 1
        })
    };

    comic = {
        loadInfo: async () => new ComicDetails({
            title: "Single Gallery",
            cover: "",
            maxPage: 3,
            thumbnails: [
                "https://example.invalid/1.jpg",
                "https://example.invalid/2.jpg",
                "https://example.invalid/3.jpg"
            ],
            tags: {}
        }),

        loadEp: async () => ({
            images: [
                { url: __BASE_URL + "/single-1.jpg" },
                { url: __BASE_URL + "/single-2.jpg" }
            ]
        }),

        onImageLoad: url => ({
            url: url,
            headers: { Referer: "https://example.com/" }
        })
    };
}
