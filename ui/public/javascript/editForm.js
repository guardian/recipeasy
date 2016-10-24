function guessQuantity(){


    $('.ingredient__detail__quantity').each(function() {
        var quant = $(this).val()
        if(quant === "") {
            var re = /(\d+(½|⅓|¼|⅔|¾)?|(½|⅓|¼|⅔|¾))/
            var parsedIngredient = $(this).parents(".ingredient").find(".ingredient__detail__parsed-ingredient").val()
            var quantityGuess = parsedIngredient.match(re)
            if(quantityGuess) {
                var cleanGuess = quantityGuess[0].replace(/½/, ".5").replace(/¼/, ".25").replace(/¾/, ".75")
                $(this).val(cleanGuess)
                $(this).attr("value", cleanGuess)
            }
        }
    })
}

function guessUnit(){
    $(".ingredient__detail__unit").each(function(){
        var unit = $(this).val()
        if(unit === "") {
            //remove known units and allow for possibility with plurals by adding optional -es / -s ending
            var re = /[\d|\s]+(cup|g|kg|oz|lb|bottle|floz|l|litre|ml|tsp|tbsp|dsp|bunch|cm|can|clove|dash|grating|handful|packet|piece|pinch|sheet|sprig|stick)e?s?\b/
            var parsedIngredient = $(this).parents(".ingredient").find(".ingredient__detail__parsed-ingredient").val()
            var unitGuess = parsedIngredient.match(re)
            if(unitGuess) {
                $(this).val(unitGuess[1])
            }
        }
    })
}

function guessComment(){
    $(".ingredient__detail__comment").each(function(){
        var comment = $(this).val()
        if(comment === "") {
            //match everything after first , or (
            var re = /(?:,\s|\()(.+$)/
            var parsedIngredient = $(this).parents(".ingredient").find(".ingredient__detail__parsed-ingredient").val()
            var commentGuess = parsedIngredient.match(re)
            if(commentGuess) {
                $(this).val(commentGuess[1])
            }
        }
    })
}

function guessItem(){
    $(".ingredient__detail__item").each(function(){
        var item = $(this).val()
        if(item === "") {
            //match words (only letters and hypens), e.g. until first ( or ,
            var re = /(\b[a-zA-Z]+(?:(-|–)(?!\d)[a-zA-Z]+)?\b\s?)+/
            var parsedIngredient = $(this).parents(".ingredient").find(".ingredient__detail__parsed-ingredient").val()
            var itemGuess = parsedIngredient.match(re)
            if(itemGuess) {
               // parse an 'a' as quantity=1
               if (itemGuess[0].search(/\ba\b/i) !== -1) {
                    $(this).siblings(".ingredient__detail__quantity").val(1)
               }
              //remove known units
              var cleanGuess = itemGuess[0].replace(/\b(cup|g|kg|oz|lb|bottle|floz|l|litre|ml|tsp|tbsp|dsp|bunch|cm|can|clove|dash|grating|handful|packet|piece|pinch|sheet|sprig|stick)e?s?\b\s?/g, "").replace(/\ba\b/i, "")
              $(this).val(cleanGuess)
            }
        }
    })
}

function guessIngredient(){
    guessQuantity()
    guessUnit()
    guessComment()
    guessItem()
}

$( document ).ready(function() {
    guessIngredient()
    $(window).keydown(function(event){
        //enter key
        if(event.keyCode == 13) {
            event.preventDefault();
            return false;
        }
    });
})


//KEYBOARD SHORT CUTS
Mousetrap.bind("i", function() {
    //last ingredient in first ingredients list
    var ingredient = $(".ingredients").first().children(".ingredient").last()
    createNewIngredient(ingredient, $.selection())
    if (ingredient.find(".ingredient__detail__parsed-ingredient").val() === "") {
      ingredient.remove()
    }
    renumIngredients.call(this, $('.ingredients'))
    guessIngredient()
})

//try to parse a whole block
Mousetrap.bind("l", function() {
    createNewIngredientList()
    var ingredients = $.selection("html").split("<br>")
    //remove html tags
    var cleanIngredients = ingredients.map(function(i) {
      return i.replace(/(<([^>]+)>)/ig,"")
    })
    cleanIngredients.forEach(function(e) {
        createNewIngredient($(".ingredient").last(), e)
    })
    $(".ingredients-list:last-child .ingredient:first-child").remove()
    renumIngredients.call(this, $('.ingredients'))
    guessIngredient()
})

Mousetrap.bind("m", function() {
    var step = $(".step").last()
    createNewStep(step, $.selection)
    if (step.find("textarea").val() === "") {
        step.remove()
    }
    renumSteps()
})

function removeElement(item, section, cb) {
    var parent = this.closest(item)
    if (!($(parent).is(":only-child")) || item == ".curated-image") {
        $(parent).remove()
        cb.call(this, $(section))
    }
}

function createNewIngredient(elemBefore, rawIngredient){
    elemBefore.after('<div class="ingredient-new">' + elemBefore.html() + "</div>")
    var newIngredient = $('.ingredient-new')
    newIngredient.find("input").val("")
    newIngredient.find(".ingredient__detail__parsed-ingredient").val(rawIngredient)
    newIngredient.removeClass('ingredient-new').addClass('ingredient')
}

function createNewIngredientList(){
    var ingredientsList = $(".ingredients-list").last()
    var ingredientsInListVal = ingredientsList.find(".ingredient").map(function(){
        return $(this).find("input").val()
    }).toArray()
    var ingredientsListIsEmpty = ingredientsInListVal.every(function(val){ return val === "" })
    ingredientsList.after('<div class="ingredients-list">' + ingredientsList.html() + "</div>")
    var newList = ingredientsList.next()
    newList.find(".ingredient").not(":first").each(function(){
        $(this).remove()
    })
    newList.find("input").val("").end()
    if (ingredientsListIsEmpty) {
        ingredientsList.remove()
    }
}

function createNewStep(elemBefore, text){
    elemBefore.after('<div class="step">' + $(".step").html() + "</div>")
    var newStep = elemBefore.next()
    newStep.find("textarea").val(text)
}

function editNameAndId(i, nameRe, idRe, newName, newId){
    var name = $(this).attr("name")
    var id = $(this).attr("id")

    if(typeof name !== typeof undefined && name !== false) {
        $(this).attr('name', $(this).attr("name").replace(nameRe, newName + i ))
    }
    if(typeof id !== typeof undefined && id !== false) {
        $(this).attr('id', $(this).attr('id').replace(idRe, newId + i))
    }
}


//ADD ELEMENTS
//step
$("body").on("click", ".step__button-add", function(){
    createNewStep($(this).parents(".step"), "")
    renumSteps()
})

//new ingredient
$("body").on("click", ".ingredient__button-add", function(){
    var ingredient = $(this).parents(".ingredients-list").find(".ingredient").last()
    createNewIngredient(ingredient, "")
    renumIngredients.call(this, $('.ingredients'))
})


//ingredient list
$("body").on("click", ".ingredients-list__button-add", function(){
    createNewIngredientList()
    renumIngredientsList.call(this)
})

//image
$("body").on("click", ".suggested-image__add", function(){
    var mediaId = $(this).parent().attr("data-media-id")
    var img = $(this).siblings("img").attr("src")
    var alt = $(this).siblings("figcaption").html()
    var index = $(".curated-images").children().length
    var newImage = '<div class="curated-image"><button type="button" class="btn btn-default btn-sm button-remove curated-image-remove"> <i class="fa fa-times" aria-hidden="true"></i></button> <input type="hidden" id="images_' + index + '_mediaId" name="images[' + index + '].mediaId" value="' + mediaId + '"> <input type="hidden" id="images_' + index + '_assetUrl" name="images[' + index + '].assetUrl"value="' + img + '"><img src="' + img + '"><input id="images_' + index + '_altText" name="images[' + index + '].altText" class="form-control" value="' + alt + '"></div>'
    $(".curated-images").append(newImage)

})


//REMOVE ELEMENTS
//step
$("body").on("click", ".step__button-remove", function(){
    removeElement.call(this, ".step", ".steps", renumSteps)
})

//ingredient
$("body").on("click", ".ingredient__button-remove", function(){
    removeElement.call(this, ".ingredient", ".ingredients", renumIngredients)
})

//ingredient list
$("body").on("click", ".ingredients-list__button-remove", function(){
    removeElement.call(this, ".ingredients-list", ".ingredients-lists", renumIngredientsList)
})

//image
$("body").on("click", ".curated-image-remove", function(){
    removeElement.call(this, ".curated-image", ".curated-images", renumImages)
})

//edit original ingredient
$("body").on("click", ".ingredient__detail__parsed-ingredient__edit-button", function(){
    var input = $(this).siblings(".ingredient__detail__parsed-ingredient")
    input.focus()
    var offset = input.val().length + 1
    input[0].setSelectionRange(offset, offset)

})

//RENUMBER ELEMENTS
function renumSteps(){
    $(".step").each(function(i){
        var num = i + 1
        $(this).children(".step__number").text(num + ".")
        $("textarea", this).each(function(){
            $(this).attr("name", "steps[" + i + "]")
            $(this).attr("id", "steps_" + i)
        })
    })
}

function renumIngredients(ingredients){
    var nameRe = /ingredients\[\d+/
    var idRe = /ingredients_\d+/

    //only renum ingredients in that block
    ingredients.find('.ingredient').each(function(i){
        $(this).find(".ingredient__detail").each(function(){
          editNameAndId.call(this, i, nameRe, idRe, "ingredients[", "ingredients_")
        })
    })
}

function renumIngredientsList(){
    var nameRe = /ingredientsLists\[\d+/
    var idRe = /ingredientsLists_\d+/

    $(".ingredients-list").each(function(i){
        var titleBox = $(this).find(".ingredients-list__title")
        titleBox.attr("name", "ingredientsLists[" + i + "]title")
        titleBox.attr("id", "ingredientsLists_" + i + "_title")
        $(this).find(".ingredient", this).each(function(){
            $(this).find(".ingredient__detail").each(function(){
                editNameAndId.call(this, i, nameRe, idRe, "ingredientsLists[", "ingredientsLists_")
            })
        })

    })
}

function renumImages(){
    var nameRe = /images\[\d+/
    var idRe = /images_\d+/

    $(".curated-image").each(function(i){
        $('input', this).each(function(){
            $(this).attr('name', "images[" + i + "]")
            $(this).attr('id', "images" + i)
        })
    })
}

$("body").on("click", "#field__serves-checkbox", function(){
  if($(this).is(":checked")){
      $(".field__serves input").each(function(){
          $(this).attr("disabled", true)
        })

    } else {
      $(".field__serves input").each(function(){
          $(this).attr("disabled", false)
        })
    }
})

$("body").on("click", 'input[name="serves.portionType"]', function(){
    $(".field__serves__quantity input").each(function(){
        $(this).attr("required", true)
    })
})

var substringMatcher = function(strs) {
  return function findMatches(q, cb) {
    var matches, substringRegex;

    // an array that will be populated with substring matches
    matches = [];

    // regex used to determine if a string contains the substring `q`
    substrRegex = new RegExp(q, 'i');

    // iterate through the pool of strings and for any string that
    // contains the substring `q`, add it to the `matches` array
    $.each(strs, function(i, str) {
      if (substrRegex.test(str)) {
        matches.push(str);
      }
    });

    cb(matches);
  };
};

var chefs = $.getJSON("/assets/javascript/credits.json", function(json){
    var chefs = json.chefs
    $('.typeahead').typeahead({
        minLength: 1,
        highlight: true,
        hint: false
        },
        {
        name: 'chefs',
        source: substringMatcher(chefs)
    });
})

