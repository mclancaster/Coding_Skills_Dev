<?php
    /*
     * Program: pupProcess
     * Purpose: Called from ImgView.html, opens / reads contents of a .txt file (Pupper_Story.txt)
     * Author: Mathieu Lancaster
     */

    
    $story = $_GET['name']; // <-- Get value from Javascript call - FileName
    
    $fp = fopen($story, "r") or die("Unable to open file!"); // <-- open File Ptr for reading, or quit in event of error
    
    $str = fread($fp, filesize($story)); // <-- read contents of file into a single string
    
    echo utf8_encode($str); // <-- return string to Javascript call (utf8 encoding for character conversion)
?>