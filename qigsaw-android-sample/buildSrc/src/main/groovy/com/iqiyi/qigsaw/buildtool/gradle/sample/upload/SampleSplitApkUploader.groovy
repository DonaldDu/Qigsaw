package com.iqiyi.qigsaw.buildtool.gradle.sample.upload

import com.iqiyi.qigsaw.buildtool.gradle.upload.SplitApkUploadException
import com.iqiyi.qigsaw.buildtool.gradle.upload.SplitApkUploader
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class SampleSplitApkUploader implements SplitApkUploader {

    @Override
    String uploadSync(Project appProject, File splitApk, String splitName) throws SplitApkUploadException {
        List<String> testOnly = appProject.extensions.splitUpload.testOnly
        boolean useTestEnv = appProject.extensions.splitUpload.useTestEnv
        if (useTestEnv) {
            return uploadSplitApk(splitApk, splitName, true)
        } else {
            return uploadSplitApk(splitApk, splitName, usingTestEnvAnyWay(testOnly, splitName))
        }
    }


    static boolean usingTestEnvAnyWay(List<String> testOnly, String splitName) {
        return testOnly != null && testOnly.contains(splitName)
    }

    /**
     * Implement this method to upload split apks to your own server.
     */
    static String uploadSplitApk(File splitApk, String splitName, boolean useTestEnv) {
        File uploaded = new File('D:\\test', splitApk.name)
        if (uploaded.exists()) uploaded.delete()
        FileUtils.copyFile(splitApk, uploaded)
        println(">>>>>Upload split " + splitName + " split apk file path: " + splitApk + " useTestEnv: " + useTestEnv)

        return 'http://192.168.50.58/' + splitApk.name
    }
}
