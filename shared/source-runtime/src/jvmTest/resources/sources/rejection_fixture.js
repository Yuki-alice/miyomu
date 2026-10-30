class RejectionFixtureSource extends ComicSource {
    name = "Rejection Fixture";
    key = "rejection_fixture";
    version = "1.0.0";
    minAppVersion = "0.1.0";
    url = "";

    async init() {
        this.failLater();
    }

    async failLater() {
        throw new Error("fire-and-forget failure");
    }

    search = {
        load: async keyword => {
            if (keyword === "fail") throw new Error("search failed");
            return {
                comics: [new Comic({ id: "ok", title: "ok" })],
                maxPage: 1
            };
        }
    };
}
