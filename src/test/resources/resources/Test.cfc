component {

    function run() {

        ftp action="open" connection="myConnection" server="localhost" username="test_user" password="testpassword";
        print.line( myConnection );

        ftp action="getcurrentdir" connection="myConnection" result="myResult";
        print.line("Connected to the server");
    }
}