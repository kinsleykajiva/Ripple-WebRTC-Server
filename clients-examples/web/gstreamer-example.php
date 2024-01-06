<!DOCTYPE html>
<html lang="en">

<head>
  <meta charset="utf-8">
  <meta content="width=device-width, initial-scale=1.0" name="viewport">

  <title>G Streamer example</title>

  <?php include_once 'includes/css.php' ?>
    <style>
        .video-controls {
  display: flex;
  justify-content: space-between;
  padding: 10px;
  background-color: #000;
  color: #fff;

}
        .video-stream {
            width: 100%;
            height: auto;
        }
.video-controls button {
  background: none;
  border: none;
  color: #fff;
  cursor: pointer;
}

.video-controls button:hover {
  color: #ddd;
}

.video-controls i {
  font-size: 1.5em;
}

        .progressBar {
            width: 100%;
            background-color: #e0e0e0; /* Material Design light grey for the background */
        }

        .progressBar::-webkit-progress-bar {
            background-color: #e0e0e0; /* Material Design light grey for the background in Webkit browsers */
        }

        .progressBar::-webkit-progress-value {
            background-color: #3f51b5; /* Material Design indigo for the progress bar in Webkit browsers */
        }

        .progressBar::-moz-progress-bar {
            background-color: #3f51b5; /* Material Design indigo for the progress bar in Firefox */
        }




    </style>
</head>

<body>
  <?php include_once 'includes/header.php' ?>



  <!-- ======= Sidebar ======= -->
  <?php include_once 'includes/left-nav.php' ?>


  <main id="main" class="main">

    <div class="pagetitle">
      <h1>G stream broadcast example demo <button class="btn btn-primary btn-sm" id="startProcessBTN" onclick="startProcess()"><i class="fas fa-file-video"></i>  Start A Stream</button></h1>
      <nav>
        <ol class="breadcrumb">
          <li class="breadcrumb-item"></li>
          <!--<li class="breadcrumb-item">Pages</li>
          <li class="breadcrumb-item active">Blank</li>-->
        </ol>
      </nav>
    </div><!-- End Page Title -->

    <section class="section">
      <div class="row">
          <div class="col-lg-12">

              <div class="card">
                  <div class="card-body">
                      <h5 class="card-title">G Streaming Broadcast</h5>
                      <p>This is aan example of how to receive streams via WebRTC.This demo show how to receive streams , so this client can get mutiple streams
                          .Each stream is handled as a thread on the client .</p>
                  </div>
              </div>

          </div>
      </div>
      <div class="row" id="streamsVids">
        <div class="col-lg-6" >

         <div class="card">
              <div class="card-body">
                <h5 class="card-title">Video Stream <span id="subheadier"></span></h5>
                <video id="localVideo" class="video-stream" autoplay playsinline muted></video>
                  <progress id="progressBar" class="progressBar" value="0" max="100" style="width: 100%"></progress>
                <div class="video-controls">
                  <!-- Controls go here -->
                    <div class="video-controls">
                      <button id="playPauseButton"><i class="fas fa-play"></i></button>
                      <button id="fastRewindButton"><i class="fas fa-backward"></i></button>
                      <button id="fastForwardButton"><i class="fas fa-forward"></i></button>
                      <input type="range" id="volumeControl" min="0" max="1" step="0.1">
                      <button style="margin-left: 10%" id="fullscreenButton"><i class="fas fa-expand"></i></button>
                        <span style="margin-left: 20%" id="progressTimerCounter">00:00</span>
                    </div>
                  <!-- Controls go here -->
                </div>
              </div>
        </div>
        </div>
      </div>
    </section>

  </main><!-- End #main -->

  <!-- ======= Footer ======= -->
  <footer id="footer" class="footer">
    <div class="copyright">
      &copy; Copyright <strong><span>NiceAdmin</span></strong>. All Rights Reserved
    </div>
    <div class="credits">
      <!-- All the links in the footer should remain intact. -->
      <!-- You can delete the links only if you purchased the pro version. -->
      <!-- Licensing information: https://bootstrapmade.com/license/ -->
      <!-- Purchase the pro version with working PHP/AJAX contact form: https://bootstrapmade.com/nice-admin-bootstrap-admin-html-template/ -->
      Designed by <a href="https://bootstrapmade.com/">BootstrapMade</a>
    </div>
  </footer><!-- End Footer -->

  <a href="#" class="back-to-top d-flex align-items-center justify-content-center"><i class="bi bi-arrow-up-short"></i></a>

  <!-- Vendor JS Files -->
  <script src="assets/vendor/apexcharts/apexcharts.min.js"></script>
  <script src="assets/vendor/bootstrap/js/bootstrap.bundle.min.js"></script>
  <script src="assets/vendor/chart.js/chart.umd.js"></script>
  <script src="assets/vendor/echarts/echarts.min.js"></script>
  <script src="assets/vendor/quill/quill.min.js"></script>
  <script src="assets/vendor/simple-datatables/simple-datatables.js"></script>
  <script src="assets/vendor/tinymce/tinymce.min.js"></script>
  <script src="assets/vendor/php-email-form/validate.js"></script>

  <!-- Template Main JS File -->
<!--  <script src="assets/js/main.js"></script>-->
  <script src="adapter.min.js"></script>
  <script src="rippleApp.js"></script>
<script>
    RippleSDK.init({
    isDebugging: true,
    iceCandidates: null,
    url: "http://localhost:8080/",
    renderGroupParentId: "streamsVids",
});
    RippleSDK.app.callbacks.tellClientOnClosed = function(){
        console.log("closed");
    }
    RippleSDK.app.callbacks.tellClientOnConnected = function(){
        console.log("connected");
    }
    RippleSDK.app.callbacks.tellClientOnMessage = function(message){
        console.log(message);
    }
    RippleSDK.app.callbacks.tellClientOnFatalError = function(err){
        console.error(err);
    }
    RippleSDK.app.callbacks.tellClientOnWebRtcEvents = function(eventMessage){
        console.log(eventMessage);
        if(eventMessage){
            const subvideoheader = document.getElementById("subvideoheader_"+eventMessage.threadRef);
            if(subvideoheader){
                subvideoheader.innerText = eventMessage.showProgress ? 'Buffering' : 'Playing';
            }
        }
    }
    RippleSDK.app.callbacks.tellClientOnStreamUIUpdates = function(eventMessage){
        console.log(eventMessage);
        if(eventMessage && eventMessage.threadRef){
            if(eventMessage.data.progressInPercentage){
                animateProgress("progressBar_"+eventMessage.threadRef, eventMessage.data.progressInPercentage);
                const progressTimerCounter = document.getElementById("progressTimerCounter_"+eventMessage.threadRef);
                if(progressTimerCounter){
                    progressTimerCounter.innerText = eventMessage.data.progressformattedTime + ":" + eventMessage.data.maxFormattedTime;
                }
                const subvideoheader = document.getElementById("subvideoheader_"+eventMessage.threadRef);
                if(subvideoheader){
                   // subvideoheader.innerText = eventMessage.data.isCompleted ? 'Finished Playing' : 'Playing';
                }
            }

        }
    }
    function animateProgress(elemId, targetValue) {
        let progressBar = document.getElementById(elemId);
        // Calculate the speed of the animation based on the difference between the target value and the current value
        let speed = (targetValue - progressBar.value) / 100;

        function frame() {
            if (progressBar.value < targetValue) {
                progressBar.value += speed; // Increase the progress bar value
                requestAnimationFrame(frame); // Call the next frame
            } else {
                progressBar.value = targetValue; // Ensure the progress bar value does not exceed the target value
            }
        }

        requestAnimationFrame(frame); // Start the animation
    }


    const streamsVids             = document.getElementById('streamsVids');
    const mediaStreamFiles=[
        "Shakespeare.mp4",
        "HeartAndSoulRiddimInstrumental.mp4",
        "MellowSleazyTmanXpressKwelinyeKeynote.mp4",
        "TheMessageRiddimMixDonCorleon.mp4"
    ];
    let chosenElements = new Set();

    function pickRandomElement() {
        if (chosenElements.size === mediaStreamFiles.length) {
            chosenElements = new Set();
        }

        let randomIndex = Math.floor(Math.random() * mediaStreamFiles.length);
        let chosenElement = mediaStreamFiles[randomIndex];

        while (chosenElements.has(chosenElement)) {
            randomIndex = (randomIndex + 1) % mediaStreamFiles.length;
            chosenElement = mediaStreamFiles[randomIndex];
        }

        chosenElements.add(chosenElement);
        return chosenElement;
    }

    function startProcess(){
        if(RippleSDK.app.features.streaming.threads.length === 0){
            streamsVids.innerHTML = "";
        }

        const fileMedia = pickRandomElement();
        console.log("selected element is "+fileMedia);
        RippleSDK.app.requestNewThread(RippleSDK.featuresAvailable.G_STREAM_BROADCAST,{
            file:fileMedia,
        });

    }



    const video             = document.getElementById('localVideo');
    const playPauseButton   = document.getElementById('playPauseButton');
    const fastRewindButton  = document.getElementById('fastRewindButton');
    const fastForwardButton = document.getElementById('fastForwardButton');
    const volumeControl     = document.getElementById('volumeControl');
    const fullscreenButton  = document.getElementById('fullscreenButton');

    fullscreenButton.addEventListener('click', () => {
        if (video.requestFullscreen) {
            video.requestFullscreen();
        } else if (video.mozRequestFullScreen) { // Firefox
            video.mozRequestFullScreen();
        } else if (video.webkitRequestFullscreen) { // Chrome, Safari and Opera
            video.webkitRequestFullscreen();
        } else if (video.msRequestFullscreen) { // IE/Edge
            video.msRequestFullscreen();
        }
    });

    // Add event listener to the play/pause button
    playPauseButton.addEventListener('click', () => {
        if (video.paused) {
            video.play();
            playPauseButton.innerHTML = '<i class="fas fa-pause"></i>';
        } else {
            video.pause();
            playPauseButton.innerHTML = '<i class="fas fa-play"></i>';
        }
    });

    // Add event listener to the fast rewind button
    fastRewindButton.addEventListener('click', () => {
        video.currentTime -= 10; // Rewind 10 seconds
    });

    // Add event listener to the fast forward button
    fastForwardButton.addEventListener('click', () => {
        video.currentTime += 10; // Fast forward 10 seconds
    });

    // Add event listener to the volume control
    volumeControl.addEventListener('input', () => {
        video.volume = volumeControl.value;
    });


</script>

</body>

</html>