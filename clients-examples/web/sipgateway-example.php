<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="utf-8">
    <meta content="width=device-width, initial-scale=1.0" name="viewport">

    <title>Sip Gateway example</title>

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
            /*height: auto;*/
            max-height: 520px;
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
        <h1>Sip Gateway example demo

        </h1>
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
                        <h5 class="card-title">Sip Gateway Broadcast</h5>
                        <p>This is an example
                            .</p>
                    </div>
                </div>

            </div>
        </div>
        <div class="row" id="streamsVids">
            <div class="col-lg-6">

                <div class="card" id="sipFormRegistrationDiv">
                    <div class="card-body">
                        <h5 class="card-title">Sip Form</h5>

                        <!-- Horizontal Form -->
                        <form onsubmit="return false;">
                            <div class="row mb-3">
                                <label for="inputHostAddress"  class="col-sm-2 col-form-label">Host Address</label>
                                <div class="col-sm-10">
                                    <input type="text" value="" placeholder="mydomain-address.com" class="form-control" id="inputHostAddress">
                                </div>
                            </div>
                            <div class="row mb-3">
                                <label for="inputUsername" class="col-sm-2 col-form-label">Username</label>
                                <div class="col-sm-10">
                                    <input type="text" value="kinsley-kajiva" placeholder="username" class="form-control" id="inputUsername">
                                </div>
                            </div>

                            <div class="row mb-3">
                                <label for="inputDisplay" class="col-sm-2 col-form-label">Display</label>
                                <div class="col-sm-10">
                                    <input type="text" value="kinsley-kajiva" class="form-control" id="inputDisplay">
                                </div>
                            </div>
                            <div class="row mb-3">
                                <label for="inputUserPassword" class="col-sm-2 col-form-label">Password</label>
                                <div class="col-sm-10">
                                    <input type="text" value="Ch1bhodhor0" class="form-control" id="inputUserPassword">
                                </div>
                            </div>

                            <div class="row mb-3">
                                <label for="inputPort" class="col-sm-2 col-form-label">Port</label>
                                <div class="col-sm-10">
                                    <input type="number" value="9099" placeholder="5060" class="form-control" id="inputPort">
                                </div>
                            </div>

                            <div class="row mb-3">
                                <label for="inputRealm" class="col-sm-2 col-form-label">Realm</label>
                                <div class="col-sm-10">
                                    <input type="text" value="Asterisk" class="form-control" id="inputRealm">
                                </div>
                            </div>


                            <div class="text-left">
                                <button type="submit" onclick="registerSipUser()" class="btn btn-primary">Register</button>
                            </div>

                        </form>

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

<a href="#" class="back-to-top d-flex align-items-center justify-content-center"><i
            class="bi bi-arrow-up-short"></i></a>

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
        renderGroupParentId: "",
    });
    RippleSDK.app.callbacks.tellClientOnClosed = function () {
        console.log("closed");
    }
    RippleSDK.app.callbacks.tellClientOnConnected = function () {
        console.log("connected");
    }
    RippleSDK.app.callbacks.tellClientOnMessage = function (message) {
        console.log(message);
    }
    RippleSDK.app.callbacks.tellClientOnFatalError = function (err) {
        console.error(err);
    }
    RippleSDK.app.callbacks.tellClientOnWebRtcEvents = function (eventMessage) {
        console.log(eventMessage);
        if (eventMessage) {
            const subvideoheader = document.getElementById("subvideoheader_" + eventMessage.threadRef);
            if (subvideoheader) {
                subvideoheader.innerText = eventMessage.showProgress ? 'Buffering' : 'Playing';
            }
        }
    }
    RippleSDK.app.callbacks.tellClientOnStreamUIUpdates = function (eventMessage) {
        console.log(eventMessage);
        if (eventMessage && eventMessage.threadRef) {


        }
    }


    const sipFormRegistrationDiv = document.getElementById('sipFormRegistrationDiv');
    const inputHostAddress = document.getElementById('inputHostAddress');
    const inputUsername = document.getElementById('inputUsername');
    const inputDisplay = document.getElementById('inputDisplay');
    const inputUserPassword = document.getElementById('inputUserPassword');
    const inputPort = document.getElementById('inputPort');
    const inputRealm = document.getElementById('inputRealm');


    function registerSipUser() {
        if (inputHostAddress.value.length === 0) {
            alert("Please enter host address");
            return;
        }
        if (inputUsername.value.length === 0) {
            alert("Please enter username");
            return;
        }

        if (inputDisplay.value.length === 0) {
            inputDisplay.value = "sipUser-" + RippleSDK.utils.uniqueIDGenerator("sipUser", 12);
            RippleSDK.utils.warn('registerToSipServer', 'no displayName set so  a random one is set: ' + displayName);

        }
        if (inputUserPassword.value.length === 0) {
            alert("Please enter user password");
            return;
        }
        if (inputPort.value.length === 0) {
            alert("Please enter port");
            return;
        }
        if (inputRealm.value.length === 0) {
            alert("Please enter Realm");
            return;
        }
        const hostAddress = inputHostAddress.value;
        const username = inputUsername.value;
        const displayName = inputDisplay.value;
        const userPassword = inputUserPassword.value;
        const port = inputPort.value;
        const realm = inputRealm.value;


        if (RippleSDK.utils.threadRefsInUseMap.length === 0) {
            // streamsVids.innerHTML = "";
        }

        RippleSDK.app.requestNewThread(RippleSDK.featuresAvailable.SIP_GATEWAY, {
            hostAddress,
            username,
            displayName,
            userPassword,
            port,
            realm
        });

    }


</script>

</body>

</html>