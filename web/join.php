<?php
    $hostname_localhost ="127.0.0.1";
    $database_localhost ="CUSTOMER";
    $username_localhost ="root";
    $password_localhost ="gill";

    $link = mysqli_connect("127.0.0.1","root","gill", "CUSTOMER")
    or
    trigger_error(mysqli_error(),E_USER_ERROR);

    if(mysqli_connect_error()){
      echo mysqli_connect_error();
    }


mysqli_set_charset($link,"utf8");

//POST 방식으로 데이터를 받아온다.
$ID=isset($_POST['ID']) ? $_POST['ID'] : '';
$PW=isset($_POST['PW']) ? $_POST['PW'] : '';
$NAME=isset($_POST['NAME']) ? $_POST['NAME'] : '';
$PATH=isset($_POST['PATH']) ? $_POST['PATH'] : '';

if ($ID !="" and $PW !="" and $NAME !=""){

    $sql="insert into LIST(ID,PW,NAME) values('$ID','$PW','$NAME')";
    $result = mysqli_query($link,$sql);

    if($result){
       echo "SQL문 처리 성공";
    }
    else{
       echo "SQL문 처리중 에러 발생 : ";
       echo mysqli_error($link);
       echo "dkr";
    }

} else {
    echo "데이터를 입력하세요 ";
}


mysqli_close($link);
?>

<?php

$android = strpos($_SERVER['HTTP_USER_AGENT'], "Android");

if (!$android){
?>

<html>
   <body>

      <form action="<?php $_PHP_SELF ?>" method="POST">
         ID: <input type = "text" name = "ID" />
         PW: <input type = "text" name = "PW" />
         Name: <input type = "text" name = "NAME" />
         PATH: <input type = "text" name = "PATH" />
         <input type = "submit" />
      </form>
       
   </body>
</html>
<?php
}
?>
