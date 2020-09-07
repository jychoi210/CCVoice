<?php

    $hostname_localhost ="127.0.0.1";

    $database_localhost ="CUSTOMER";

    $username_localhost ="root";

    $password_localhost ="gill";


    $localhost = mysqli_connect("127.0.0.1","root","gill", "CUSTOMER")
    or
    trigger_error(mysqli_error(),E_USER_ERROR);

    if(mysqli_connect_error()){
      echo mysqli_connect_error();
    }

    $ID = $_POST['ID'];
    $PW = $_POST['PW'];

    $query_search = "select * from LIST where ID = '".$ID."' AND PW = '".$PW. "'";
    $query_exec = mysqli_query($localhost, $query_search) or die(mysqli_error());
    $rows = mysqli_num_rows($query_exec);

    if($rows >=1) {
      $dir = '/home/gill/customer_list/'.$ID.'/';

      if(is_dir($dir)){
        echo "User Found";
      } else {
        $old = umask(0);
        mkdir($dir, 0777, true);
        umask($old);

        $query_insert="update LIST set PATH = '".$dir."' where ID = '".$ID."'" ;
        $query_exec = mysqli_query($localhost, $query_insert) or die(mysqli_error());
          
        echo "User Found";
      }
    }

    else  {
      echo "No Such User Found";
     echo mysqli_connect_error();
      }

mysqli_close($localhost);

?>
