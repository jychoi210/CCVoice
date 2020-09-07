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

    //POST 방식으로 데이터를 받아온다.
    $ID = $_POST['ID'];

    $query_search = "select * from LIST where ID = '".$ID."'";
    $query_exec = mysqli_query($localhost, $query_search) or die(mysqli_error());
    $rows = mysqli_num_rows($query_exec);

    $result = mysqli_query($link,$sql);

    if($rows>=1){
      echo "2";
      exit;

      if($result){
         echo "SQL문 처리 성공";
      }
      else{
         echo "SQL문 처리중 에러 발생 : ";
         echo mysqli_error($link);
      }
    }
?>
