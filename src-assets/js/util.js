(function () {
  'strict'

  var util = {}

  /**
   * www.hellowork.go.jp updated around 2016/03/20 and old search object doesn't work
   * any more. The user has old search (query params) data in their devices and we need
   * to update it when using it.
   * @param {object} searchObject The search object
   * @return {object}
   */
  util.updateOldSearchObject = function (searchObject) {

    searchObject = searchObject || {}

    if (searchObject.todofuken1 != null || searchObject.todofuken == null) {
      // old todofuken1 became todofuken
      searchObject.todofuken = searchObject.todofuken1
    }

    if (searchObject.kiboShokushu != null || searchObject.kiboShokushu1 == null) {
      // old kiboShokushu became kiboShokushu1,2,3
      searchObject.kiboShokushu1 = searchObject.kiboShokushu
    }

    if (searchObject.kiboSangyo != null || searchObject.kiboSangyo1 == null) {
      // old kiboSangyo became kiboSangyo1,2,3
      searchObject.kiboSangyo1 = searchObject.kiboSangyo
    }

    if (searchObject.hakenUkeoiNozoku != null && searchObject.hakenUkeoi == null) {
      // old hakenUkeoiNozoku flag became hakenUkeoi array, ["3", "4"] means the same as excluding haken/ukeoi
      searchObject.hakenUkeoi = ["3", "4"]
    }
  }

  window.util = util
}())
