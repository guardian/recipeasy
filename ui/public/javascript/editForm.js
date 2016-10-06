function guessQuantity(){
    $('.ingredient__detail__quantity').each(function() {
        var quant = $(this).val()
        if(quant === "") {
            var re = /\d+/
            var parsedIngredient = $(this).parents(".ingredient").find(".ingredient__detail__parsed-ingredient").val()
            var quantityGuess = parsedIngredient.match(re)
            if(quantityGuess) {
                $(this).val(quantityGuess[0])
                $(this).attr("value", quantityGuess[0])
            }
        }
    })
}

function guessUnit(){
    $(".ingredient__detail__unit").each(function(){
        var unit = $(this).val()
        if(unit === "") {
            var re = /(g|ml|l|oz|floz|cup|tsp|tbsp|pinch|handful|grating)\s/
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
            var re = /,(.+$)/
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
            var re = /[\d+]?[g|ml|l|oz|floz|cup|tsp|tbsp|pinch|handful|grating]?\s([^,]+)/
            var parsedIngredient = $(this).parents(".ingredient").find(".ingredient__detail__parsed-ingredient").val()
            var itemGuess = parsedIngredient.match(re)
            if(itemGuess) {
                $(this).val(itemGuess[1])
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
})


//KEY BOARD SHORT CUTS
Mousetrap.bind("i", function() {
    //last ingredient in first ingredients list
    var ingredient = $(".ingredients").first().children(".ingredient").last()
    console.log(ingredient, "ingredient")
    createNewIngredient(ingredient, $.selection())
    renumIngredients.call(this, $('.ingredients'))
    guessIngredient()
})

//try to parse a whole block
Mousetrap.bind("I", function() {
    var ingredients = $.selection("html").split("<br>")
    //remove html tags
    var cleanIngredients = ingredients.map(function(i) {
      return i.replace(/(<([^>]+)>)/ig,"")
    })
    cleanIngredients.forEach(function(e) {
        createNewIngredient($(".ingredient").last(), e)
    })
    renumIngredients.call(this, $('.ingredients'))
    guessIngredient()
})

Mousetrap.bind("s", function() {
  createNewStep($(".step").last(), $.selection)
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
    var ingredientsList = $(".ingredients-list").last()
    ingredientsList.after('<div class="ingredients-list">' + ingredientsList.html() + "</div>")
    var newList = ingredientsList.next()
    newList.find(".ingredient").not(":first").each(function(){
        $(this).remove()
    })
    newList.find("input").val("").end()
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

