const utils = require("./utils");

(async () => {
    try {
        const latestVersionInfo = await utils.getAppVersion()
        let releaseInfo = await utils.getLatestRelease()
        if (releaseInfo.name !== latestVersionInfo["package_version"]) {
            await utils.runWorkflow(41172724)
        }
    } catch (e) {
        console.log(e)
    }
})()
