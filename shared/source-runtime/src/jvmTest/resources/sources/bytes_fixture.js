class BytesFixtureSource extends ComicSource {
    name = "Bytes Fixture";
    key = "bytes_fixture";
    version = "1.0.0";
    minAppVersion = "0.1.0";
    url = "";

    search = {
        load: async () => {
            const response = await Network.fetchBytes(
                "GET",
                __BASE_URL + "/binary",
                { "X-Miyomu-Test": "bytes" }
            );
            const body = new Uint8Array(response.body);
            return {
                comics: [new Comic({
                    id: String(response.status),
                    title: Array.from(body).join(","),
                    subtitle: response.headers["x-miyomu-header"] || ""
                })],
                maxPage: 1
            };
        }
    };
}
