# How to debug when running locally

The application is available for remote debugging when started using the 'bootRunLocal' gradle task (e.g. ap-tools server start --local-api)

To debug the locally running API, do the following in the approved-premises-api Intellij Project:

1. Click 'Run' -> 'Edit Configurations...'
2. Click on the '+' symbol at the top left
3. Choose 'Remote JVM Debug'
4. In the Run Configuration set the following:
   1. Name can be whatever you like e.g. 'Remote API'
   2. Host and port should be localhost:32323
   3. Module classpath should be 'approved-premises-api'
5. After starting the API locally, run the new Configuration using 'Run' -> 'Run...'

If successful, the Debug Tool Window should appear saying "Connected to the target VM, address: 'localhost:32323', transport: 'socket'".

Break points can now be used in Intellij to debug