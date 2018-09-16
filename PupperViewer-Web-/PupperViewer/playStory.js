/**
 * ImgView: JavaScript Purpose: Loads Story file, and iterates through story,
 * based on button clicks Author: Mathieu Lancaster
 */

// DECLARATIONS
var intro = "Inspired by a true story!\n\nUse the 'Next' / 'Previous' buttons to read through this kid-friendly story of my favorite Shelter Pupper :D";
var storyName = "Pupper_Story.txt";
var storyIndex = 0;
var songIndex = 0;
var strStory = "";
var storyParts;
var imgParts = [ "intro", "pup_shelter", "pup_hero", "pup_bone", "pup_scared",
		"pup_skeptical", "pup_bedHog", "pup_lounge", "pup_legSit", "pup_math",
		"pup_destroyer", "pup_upClose", "pup_nMe", "theEnd" ];
var songList = [ {
	songName : "Jenseits Von Gut Und Bose",
	fileName : "Dee_Yan-Key-jenseits",
	artist : "Dee Yan-Key"
}, {
	songName : "Sweet Dreams",
	fileName : "Lobo_Loco-sweetDreams",
	artist : "Lobo Loco"
} ];
var songWeb = "The Free Music Archive";
var songWebRes = songWeb.link("https://www.freemusicarchive.org");
var textAreaEl = document.getElementById("textStory");
var imgEl = document.getElementById("imgStory");
var audEl = document.getElementById("musPlay");
var xhttp;

/*
 * Function: getStory Purpose: sends xhttp request to pupProcess.php, to
 * retreive contents of story text-file
 */
function getStory() {
	xhttp = new XMLHttpRequest(); // <-- create request
	changeSong(); // <-- load first song

	// Each time the request-state changes...
	xhttp.onreadystatechange = function() {

		// readyState(4) = Operation complete, status(200) = request succesfull
		if (this.readyState == 4 && this.status == 200) {

			strStory = xhttp.responseText; // <-- retrieve text from query
			storyParts = strStory.split("<END>"); // <-- break string into array by '<END>' delimeter (was written into .txt file)
			storyParts.unshift(intro); // <-- add intro portion

			// for each string element in array...
			for (var i = 0; i < storyParts.length; i++) {
				storyParts[i] = storyParts[i].trim(); // <-- trim any leading/trailing white-spaces
			}
			textAreaEl.value = storyParts[0]; // <-- set opening window to first string (Intro)
		}
	}
	xhttp.open("GET", "pupProcess.php?name=" + storyName, true); // <-- Open .php file, passing 'name' parameter to open file
	xhttp.send(); // <-- sent request
}

/*
 * Function: nextPhoto Purpose: advances story to the next Image/Dialogue
 * option, unless @ end of arrays (will do nothing)
 */
function nextPhoto() {
	storyIndex++; // <-- increase index

	// if we've reached the end of the array...
	if (storyIndex == storyParts.length) {
		storyIndex--; // <-- decrement index to stay in-bounds

		// if another story-piece is available...
	} else {
		textAreaEl.value = storyParts[storyIndex]; // <-- change textArea to
													// new dialogue
		imgEl.src = "pup_imgs/" + imgParts[storyIndex] + ".jpg"; // <--
																	// update
																	// image
	}
}

/*
 * Function: prevPhoto Purpose: moves story backwards, to the prior
 * Image/Dialogue option, unless @ beginning of arrays (will do nothing)
 */
function prevPhoto() {
	storyIndex--; // <-- decrement index

	// if we've moved before alloted array indices...
	if (storyIndex < 0) {
		storyIndex++; // <-- increment index to stay in-bounds

		// if a prior story-piece is available...
	} else {
		textAreaEl.value = storyParts[storyIndex]; // <-- change textArea to new dialogue
		imgEl.src = "pup_imgs/" + imgParts[storyIndex] + ".jpg"; // <-- update image
	}
}

/*
 * Function: changeSong Purpose: called @ end of media, switches to next
 * available song
 */
function changeSong() {

	// change index based on current index
	if (songIndex == 0) {
		songIndex = 1;
	} else {
		songIndex = 0;
	}
	audEl.src = "music/" + songList[songIndex].fileName + ".mp3"; // <-- load new audio

	// update song information
	document.getElementById("songInfo").value = "Song: "+ songList[songIndex].songName + "\nArtist: "
														+ songList[songIndex].artist;
}