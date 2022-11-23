const fs = require("fs")
const axios = require("axios").create({
    validateStatus: _ => true
})

const repo = "HolographicHat/GetToken"
const token = process.env.GHP_TOKEN

const getAppVersion = async () => {
    const latestVersionRsp = await axios.get("https://api-takumi.mihoyo.com/ptolemaios/api/getLatestRelease", {
        headers: {
            "x-rpc-client_type": "2",
            "x-rpc-app_version": "2.37.1",
            "x-rpc-channel": "miyousheluodi"
        }
    })
    return latestVersionRsp.data.data
}

const getLatestRelease = async () => {
    const repoLatestVersionRsp = await axios.get(`https://api.github.com/repos/${repo}/releases/latest`)
    return repoLatestVersionRsp.data
}

const createRelease = async (name, desc) => {
    const result = await axios.post(`https://api.github.com/repos/${repo}/releases`, {
        tag_name: name,
        name: name,
        body: desc,
        generate_release_notes: true
    }, {
        headers: {
            "Authorization": `Bearer ${token}`,
        }
    })
    return result.data
}

const uploadReleaseAsset = async (url, path) => {
    const uri = url.substring(0, url.indexOf("{")) + "?name=" + require("path").basename(path)
    const size = fs.statSync(path).size
    const data = fs.readFileSync(path)
    await axios.post(uri, data, {
        headers: {
            "Content-Type": "application/vnd.android.package-archive",
            "Content-Length": `${size}`,
            "Authorization": `Bearer ${token}`,
        }
    })
}

module.exports = { getAppVersion, getLatestRelease, createRelease, uploadReleaseAsset }
