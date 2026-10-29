// AppleCMS (苹果CMS V10) anime template source — one script covers every site exposing the
// standard /api.php/provide/vod/ JSON API. Per-site authors copy this file and edit DEFAULTS.
// Repository entries may also override key/name/api via the index `config` object, which the
// runtime injects as globalThis.__MIYOMU_CONFIG (D9). Inline values are the fallback defaults.
const DEFAULTS = {
    key: "applecms_demo",
    name: "AppleCMS Demo",
    version: "1.0.0",
    api: __BASE_URL + "/api.php/provide/vod/",
    // Line whose from matches this substring becomes line 0 (play route priority).
    preferFrom: "m3u8",
};
const CONFIG = Object.assign({}, DEFAULTS, globalThis.__MIYOMU_CONFIG || {});
CONFIG.headers = {
    Referer: CONFIG.api.replace(/^(https?:\/\/[^/]+).*$/, "$1") + "/",
    "User-Agent": "Mozilla/5.0 (Miyomu AnimeSource)"
};

class AppleCmsTemplateSource extends AnimeSource {
    name = CONFIG.name;
    key = CONFIG.key;
    version = CONFIG.version;
    url = CONFIG.api;

    async search(query, page) {
        const response = await Network.get(
            CONFIG.api + "?ac=videolist&wd=" + encodeURIComponent(query) + "&pg=" + (page || 1),
            CONFIG.headers
        );
        const payload = JSON.parse(response.body);
        return {
            list: (payload.list || []).map(vod => ({
                id: String(vod.vod_id),
                title: vod.vod_name || "",
                cover: vod.vod_pic || null,
                subtitle: vod.vod_remarks || null
            })),
            maxPage: payload.pagecount || 1
        };
    }

    async detail(id) {
        const response = await Network.get(
            CONFIG.api + "?ac=videolist&ids=" + encodeURIComponent(id),
            CONFIG.headers
        );
        const payload = JSON.parse(response.body);
        const vod = (payload.list || [])[0] || {};

        // Lines: vod_play_from/vod_play_url carry one group per play route.
        const lines = Std.parseAppleCmsPlay(vod.vod_play_from, vod.vod_play_url);
        const preferred = lines.filter(line => line.from.indexOf(CONFIG.preferFrom) >= 0);
        const ordered = preferred.concat(lines.filter(line => line.from.indexOf(CONFIG.preferFrom) < 0));

        const episodes = [];
        ordered.forEach((line, lineIndex) => {
            line.episodes.forEach(episode => {
                episodes.push({
                    key: line.from + ":" + episode.url,
                    name: episode.name,
                    url: episode.url,
                    line: lineIndex,
                    number: Std.parseEpisodeNumber(episode.name)
                });
            });
        });

        return {
            title: vod.vod_name || id,
            cover: vod.vod_pic || null,
            description: vod.vod_content || null,
            tags: { type: String(vod.vod_class || "").split(",").filter(Boolean) },
            updateTime: vod.vod_time || null,
            episodes: episodes
        };
    }

    async resolve(episodeUrl) {
        // AppleCMS hands out direct media urls; the anti-leech headers are site policy.
        return {
            urls: [{
                url: episodeUrl,
                quality: null,
                type: /\.m3u8($|\?)/i.test(episodeUrl) ? "hls" : "mp4"
            }],
            headers: CONFIG.headers
        };
    }
}
