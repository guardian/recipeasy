$("body").on("click", ".button-add", function(){

    var steps = $(this).parents(".steps")
    var step = $(this).parent()
    var template = $(".step")
    step.after('<div class="flex step step_n">' + template.html() + "</div>")

    renumber()
})

$("body").on("click", ".button-remove", function(){

  $(this).parent().remove()
  renumber()

})

function renumber(){
    $('.step').each(function(i){
        $('textarea', this).each(function(){
            $(this).attr('name', "steps[" + i + "]")
            $(this).attr('id', "steps_" + i)
        })
    })
}
