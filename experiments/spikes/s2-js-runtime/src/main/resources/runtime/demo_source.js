// Fixture source for the S2 spike. Endpoints are served by FixtureServer.
class DemoSource extends ComicSource {
    name = "Demo Fixture";
    key = "demo";
    version = "1.0.0";
    url = "";

    search = {
        load: async (keyword, options, page) => {
            const response = await Network.get(
                __BASE_URL + "/api/search?q=" + encodeURIComponent(keyword) + "&page=" + page
            );
            if (response.status !== 200) {
                throw new Error("HTTP " + response.status);
            }
            const data = JSON.parse(response.body);
            return {
                comics: data.items.map(item => new Comic(item)),
                maxPage: data.totalPages
            };
        }
    };

    comic = {
        loadInfo: async (id) => {
            const response = await Network.get(__BASE_URL + "/api/comics/" + id);
            const data = JSON.parse(response.body);
            return new ComicDetails(data);
        },
        loadEp: async (comicId, epId) => {
            const response = await Network.get(
                __BASE_URL + "/api/comics/" + comicId + "/chapters/" + epId
            );
            const data = JSON.parse(response.body);
            return { images: data.images.map(path => __BASE_URL + path) };
        }
    };
}

const demoSourceInstance = new DemoSource();
ComicSource.sources[demoSourceInstance.key] = demoSourceInstance;
