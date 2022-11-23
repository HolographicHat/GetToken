const fs = require("fs")
const utils = require("./utils");

(async () => {
    try {
        const latestVersionInfo = await utils.getAppVersion()
        let releaseInfo = await utils.getLatestRelease()
        if (releaseInfo.name !== latestVersionInfo["package_version"]) {
            releaseInfo = await utils.createRelease(latestVersionInfo["package_version"], undefined)
        }
        for (const path of fs.readdirSync("out")) {
            console.log(`upload ${path}...`)
            await utils.uploadReleaseAsset(releaseInfo["upload_url"], path)
        }
    } catch (e) {
        console.log(e)
    }
})()
