/**
 * @author Eddie
 * @date 2026-06-20
 */

import {createRouter, createWebHistory} from 'vue-router'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            redirect: '/chat',
        },
        {
            path: '/chat',
            name: 'chat',
            components: {
                default: () => import('../views/ChatView.vue'),
                panel: () => import('../components/chat/ChatSidebar.vue'),
            },
        },
        {
            path: '/chat/:id',
            name: 'chat-detail',
            components: {
                default: () => import('../views/ChatView.vue'),
                panel: () => import('../components/chat/ChatSidebar.vue'),
            },
        },
        {
            path: '/agent',
            name: 'agent',
            component: () => import('../views/AgentView.vue'),
        },
        {
            path: '/roles',
            name: 'roles',
            component: () => import('../views/RoleListView.vue'),
        },
        {
            path: '/roles/:id',
            name: 'role-edit',
            component: () => import('../views/RoleEditView.vue'),
        },
        {
            path: '/settings',
            name: 'settings',
            component: () => import('../views/SettingsView.vue'),
        },
    ],
})

export default router
