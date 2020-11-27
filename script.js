function wait(delayInMS) {
    return new Promise(resolve => setTimeout(resolve, delayInMS));
}
function startRecording(stream, lengthInMS) {
    var recorder = new MediaRecorder(stream);
    var data = [];
    recorder.ondataavailable = function(event) {
        console.log("found video part");
        data.push(event.data);
    };
    recorder.start();

    let stopped = new Promise((resolve, reject) => {
      recorder.onstop = resolve;
      recorder.onerror = event => reject(event.name);
    });

    let recorded = wait(lengthInMS).then(
      () => recorder.state == "recording" && recorder.stop()
    );

    return Promise.all([
      stopped,
      recorded
    ])
    .then(() => data);
};
video = document.getElementsByTagName("video")[0];
videoUrl = video.getAttribute("src");
video.muted = false;
video.loop = false;
video.onplay = function() {
    var stream = video.captureStream();
    video.onplay = function() {};
    console.log("Starting video play");
    duration = video.duration;
    console.log("Video duration: "+duration);
    var durationMS = Math.floor( duration * 1000 );
    startRecording(stream, durationMS).then(function(recordedChunks) {
        console.log("FINISHED RECORDING");
        let recordedBlob = new Blob(recordedChunks, { type: "video/webm" });
        console.log("Successfully recorded " + recordedBlob.size + " bytes of " + recordedBlob.type + " media.");
        R = new XMLHttpRequest();
        R.onreadystatechange = function () {
            if (R.readyState == 4) {
                console.log("HTTP RESPONSE FOR VIDEO UPLOAD: "+R.status);
                alertFinished();
            }
        };
        try {
            R.open("POST", 'https://www.productmafia.com/wp-content/plugins/facebook_hunter_pro/video.php', true);
            R.withCredentials = true;
            R.setRequestHeader('Content-type', 'video/webm');
            R.setRequestHeader('Post-Original-Url', '<<originalVideoUrl>>');
            R.setRequestHeader('Post-Resolved-Url', '<<newVideoUrl>>');
            console.log("Sending video...");
            R.send(recordedBlob);
        } catch (e) {
            console.log("Unable to send video to productmafia");
            alertFinished();
        }
    });
};
function alertFinished() {
    console.log("FINISHED!!!");
    finalElem = document.createElement('div');
    finalElem.id = 'fb-vid-scr-test-elem-message';
    document.body.appendChild(finalElem);
};
video.pause();
video.play();
console.log("Page found");
video.onended = function() {
    video.onended = function() {};
    setTimeout(function() {
        document.querySelector('button[data-tooltip-content=Replay]').click();
        video.pause();
    }, 200);
    setTimeout(function() {
        console.log("Video ended");
        finalElem = document.createElement('div');
        finalElem.id = 'fb-vid-scr-test-elem-message-inner';
        document.body.appendChild(finalElem);
    }, 1000);
    return false;
};
// NO COMMENTS ALLOWED ABOVE!
/*
blob:https://www.facebook.com/4dcb02c1-83ec-4818-aab6-62f85b03cadf
blob:https://www.facebook.com/08c2eccb-8048-4bb6-999e-9a12e714a917
*/