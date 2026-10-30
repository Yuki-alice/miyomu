class CookieFixtureSource extends ComicSource {
    name = "Cookie Fixture";
    key = "cookie_fixture";
    version = "1.0.0";
    minAppVersion = "0.1.0";
    url = "";

    search = {
        load: async () => {
            Network.setCookies(__BASE_URL, [new Cookie({ name: "sid", value: "abc123" })]);
            const cookies = await Network.getCookies(__BASE_URL);
            const first = await Network.get(__BASE_URL + "/cookie-echo");
            Network.deleteCookies(__BASE_URL);
            const second = await Network.get(__BASE_URL + "/cookie-echo");
            return {
                comics: [new Comic({
                    id: String(cookies.length),
                    title: first.body,
                    subtitle: second.body
                })],
                maxPage: 1
            };
        }
    };
}
