import { defineStore } from 'pinia'

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: []
  }),

  getters: {
    count: (state) => state.items.length,
    orderItems: (state) => state.items.map(i => ({ dishId: i.dishId, spiciness: i.spiciness }))
  },

  actions: {
    addItem(dish) {
      if (!this.items.find(i => i.dishId === dish.id)) {
        this.items.push({
          dishId: dish.id,
          dishName: dish.name,
          dishDescription: dish.description,
          imagePath: dish.imagePath,
          spiciness: dish.spiciness || 'none'
        })
        uni.showToast({ title: `已加入: ${dish.name}`, icon: 'success', duration: 1000 })
      } else {
        uni.showToast({ title: '已在列表中', icon: 'none', duration: 1000 })
      }
    },

    removeItem(dishId) {
      this.items = this.items.filter(i => i.dishId !== dishId)
    },

    clear() {
      this.items = []
    }
  }
})
