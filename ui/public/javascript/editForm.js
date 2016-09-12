$( document ).ready(function() {
    //guess ingredient quantities when field empty
    $('.ingredient__detail__quantity', this).each(function() {
        var quant = $(this).val()
        if(typeof quant !== typeof undefined) {
            var re = /\d+/
            var parsedIngredient = $(this).siblings(".ingredients__detail__parsed-ingredient").text()
            var quantityGuess = parsedIngredient.match(re)
            if(quantityGuess) {
                $(this).val(quantityGuess[0])
                $(this).attr("value", quantityGuess[0])
            }
        }
    })

    //guess ingredient unit when field empty
    $(".ingredients__detail__unit", this).each(function(){
        var unit = $(this).val()
        if(typeof unit !== typeof undefined) {
            var re = /(g|ml|l|oz|floz|cup|tsp|tbsp|pinch|handful|grating)\s/
            var parsedIngredient = $(this).siblings(".ingredients__detail__parsed-ingredient").text()
            var unitGuess = parsedIngredient.match(re)
            if(unitGuess) {
                $(this).val(unitGuess[1])
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
    $(this).parent().next().find("span").text("")
    var ingredients = $(this).parent().parent()

    renumIngredients.call(this, ingredients)
})

//ingredient list
$("body").on("click", ".ingredients-list__button-add", function(){
    var ingredientsList = $(this).parent()
    ingredientsList.after('<div class="ingredients-list">' + ingredientsList.html() + "</div>")
    var newList = ingredientsList.next()
    newList.find(".ingredient").not(":first").each(function(){
        $(this).remove()
    })
    newList.find("span").text("")
    renumIngredientsList.call(this)
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
