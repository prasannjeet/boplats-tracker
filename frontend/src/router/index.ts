import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  { path: '/', name: 'home', component: () => import('@/views/HomeView.vue') },
  { path: '/listings', name: 'listings', component: () => import('@/views/ListingsView.vue') },
  { path: '/listings/:internalId', name: 'detail', component: () => import('@/views/DetailView.vue'), props: true },
  { path: '/saved', name: 'saved', component: () => import('@/views/SavedView.vue') },
  { path: '/types/:typeId', name: 'type-detail', component: () => import('@/views/TypeDetailView.vue'), props: true },
  { path: '/:catchAll(.*)', redirect: '/' },
];

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, saved) {
    if (saved) return saved;
    if (to.path !== from.path) return { top: 0 };
    return undefined;
  },
});
