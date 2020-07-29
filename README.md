# Passport reader

This project represents an NFC passport reader written in Kotlin. It's based on the JMRTD 0.7.16 library.

When starting the app, it will directly start with LoggingActivity skipping all inputs and taking them as default (specified inside InputActivity). If you want to start the application always with input screen, comment out startReadingButton() function invocation in onCreate of the InputActivity.

Before starting the app, make sure that NFC feature is enabled, if it is not the case, inside the app, go back (to the input screen), turn on NFC, and press confirm again.

This version of the reader is focused around authentication, therefore, only very limited information if being read from the document, but it can be easily extended to your needs by adding necessary code to the extractPrivateData() function.

And last but not the least, PACE authentication is based on CAN, so if you need to authenticate with PACE but using MRZ data simply replace paceKey with bacKey when calling doPace.
