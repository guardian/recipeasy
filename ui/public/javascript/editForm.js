$( document ).ready(function() {
    //guess ingredient quantities when field empty
    $('.ingredient__detail__quantity', this).each(function() {
        var quant = $(this).val()
        if(quant === "") {
            var re = /\d+/
            var parsedIngredient = $(this).siblings(".ingredient__detail__parsed-ingredient").text()
            var quantityGuess = parsedIngredient.match(re)
            if(quantityGuess) {
                $(this).val(quantityGuess[0])
                $(this).attr("value", quantityGuess[0])
            }
        }
    })

    //guess ingredient unit when field empty
    $(".ingredient__detail__unit", this).each(function(){
        var unit = $(this).val()
        if(unit === "") {
            var re = /(g|ml|l|oz|floz|cup|tsp|tbsp|pinch|handful|grating)\s/
            var parsedIngredient = $(this).siblings(".ingredient__detail__parsed-ingredient").text()
            var unitGuess = parsedIngredient.match(re)
            if(unitGuess) {
                $(this).val(unitGuess[1])
            }
        }
    })

    //guess comment when field empty
    $(".ingredient__detail__comment", this).each(function(){
        var comment = $(this).val()
        if(comment === "") {
            var re = /,(.+$)/
            var parsedIngredient = $(this).siblings(".ingredient__detail__parsed-ingredient").text()
            var commentGuess = parsedIngredient.match(re)
            if(commentGuess) {
                $(this).val(commentGuess[1])
            }
        }
    })

    //guess ingredient when field empty
    $(".ingredient__detail__item", this).each(function(){
        var item = $(this).val()
        if(item === "") {
            var re = /[\d+]?[g|ml|l|oz|floz|cup|tsp|tbsp|pinch|handful|grating]\s([^,]+)/
            var parsedIngredient = $(this).siblings(".ingredient__detail__parsed-ingredient").text()
            var itemGuess = parsedIngredient.match(re)
            if(itemGuess) {
                $(this).val(itemGuess[1])
            }
        }
    })
})


function removeParent(cb) {
    if (!$(this).parent().is(":only-child")) {
        var section = $(this).parent().parent()
        $(this).parent().remove()
        cb.call(this, section)
    }
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

    var steps = $(this).parents(".steps")
    var step = $(this).parent()
    var template = $(".step")
    step.after('<div class="flex step">' + template.html() + "</div>")
    var newStep = step.next()
    newStep.find("textarea").val("")

    renumSteps()
})

//new ingredient
$("body").on("click", ".ingredient__button-add", function(){

    var ingredient = $(this).parent()
    ingredient.after('<div class="flex ingredient">' + ingredient.html() + "</div>")
    $(this).parent().next().find("textarea").val("")
    var ingredients = $(this).parent().parent()

    renumIngredients.call(this, ingredients)
})

//ingredient list
$("body").on("click", ".ingredients-list__button-add", function(){
    var ingredientsList = $(".ingredients-list")
    ingredientsList.after('<div class="ingredients-list">' + ingredientsList.html() + "</div>")
    var newList = ingredientsList.next()
    newList.find(".ingredient").not(":first").each(function(){
        $(this).remove()
    })
    newList.find("span").text("")
    renumIngredientsList.call(this)
})

//image
$("body").on("click", ".suggested-image__add", function(){
    var mediaId = $(this).parent().attr("data-media-id")
    var img = $(this).siblings("img").attr("src")
    var alt = $(this).siblings("figcaption").html()
    var index = $(".curated-images").children().length
    var newImage = '<div class="curated-images__image"> <input type="hidden" id="images_' + index + '_mediaId" name="images[' + index + '].mediaId" value="' + mediaId + '"> <img id="images_' + index + '_assetUrl" name="images[' + index + '].assetUrl" class="thumbnail" src="' + img + '"> <textarea id="images_' + index + '_altText" name="images[' + index + '].altText" class="form-control">' + alt + '</textarea> </div>'
    $(".curated-images").append(newImage)

})


//REMOVE ELEMENTS
//step
$("body").on("click", ".step__button-remove", function(){
    removeParent.call(this, renumSteps)
})

//ingredient
$("body").on("click", ".ingredient__button-remove", function(){
    removeParent.call(this, renumIngredients)
})

//ingredient list
$("body").on("click", ".ingredients-list__button-remove", function(){
    removeParent.call(this, renumIngredientsList)
})


//RENUMBER ELEMENTS
function renumSteps(){
    $('.step').each(function(i){
        $('textarea', this).each(function(){
            $(this).attr('name', "steps[" + i + "]")
            $(this).attr('id', "steps_" + i)
        })
    })
}

function renumIngredients(ingredients){
    var nameRe = /ingredients\[\d+/
    var idRe = /ingredients_\d+/

    //only renum ingredients in that block
    ingredients.find('.ingredient').each(function(i){
        $(this).children().each(function(){
          editNameAndId.call(this, i, nameRe, idRe, "ingredients[", "ingredients_")
        })
    })
}

function renumIngredientsList(){
    var nameRe = /ingredientsLists\[\d+/
    var idRe = /ingredientsLists_\d+/

    $(".ingredients-list").each(function(i){
        var titleBox = $(this).children("input")
        titleBox.attr("name", "ingredientsLists[" + i + "]title")
        titleBox.attr("id", "ingredientsLists_" + i + "_title")
        $(this).find(".ingredient", this).each(function(){
            $(this).children().each(function(){
                editNameAndId.call(this, i, nameRe, idRe, "ingredientsLists[", "ingredientsLists_")
            })
        })

    })
}
