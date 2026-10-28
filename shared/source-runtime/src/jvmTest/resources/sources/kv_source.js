class KvFixtureSource extends ComicSource {
    name = "KV Fixture";
    key = "kv_source";
    version = "1.0.0";
    url = "";

    search = {
        load: async () => {
            const previous = this.loadData("count") || "0";
            const next = String(Number(previous) + 1);
            this.saveData("count", next);
            return {
                comics: [new Comic({ id: next, title: "count " + next, cover: "" })],
                maxPage: 1
            };
        }
    };
}
