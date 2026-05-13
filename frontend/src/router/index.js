import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import LoginView from '../views/LoginView.vue'
import LayoutView from '../layouts/LayoutView.vue'
import DashboardView from '../views/DashboardView.vue'
import StudentCourseView from '../views/student/StudentCourseView.vue'
import StudentTimetableView from '../views/student/StudentTimetableView.vue'
import StudentScoreView from '../views/student/StudentScoreView.vue'
import TeacherClassView from '../views/teacher/TeacherClassView.vue'
import TeacherScoreView from '../views/teacher/TeacherScoreView.vue'
import AdminStudentView from '../views/admin/AdminStudentView.vue'
import AdminTeacherView from '../views/admin/AdminTeacherView.vue'
import AdminCourseView from '../views/admin/AdminCourseView.vue'
import AdminStatisticsView from '../views/admin/AdminStatisticsView.vue'
import AdminSelectionRequestView from '../views/admin/AdminSelectionRequestView.vue'
import PasswordView from '../views/PasswordView.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { public: true }
  },
  {
    path: '/',
    component: LayoutView,
    meta: { roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
    children: [
      {
        path: '',
        name: 'dashboard',
        component: DashboardView,
        meta: { roles: ['ADMIN', 'TEACHER', 'STUDENT'] }
      },
      {
        path: 'password',
        name: 'password',
        component: PasswordView,
        meta: { roles: ['ADMIN', 'TEACHER', 'STUDENT'] }
      },
      {
        path: 'student/courses',
        name: 'student-courses',
        component: StudentCourseView,
        meta: { roles: ['STUDENT'] }
      },
      {
        path: 'student/timetable',
        name: 'student-timetable',
        component: StudentTimetableView,
        meta: { roles: ['STUDENT'] }
      },
      {
        path: 'student/scores',
        name: 'student-scores',
        component: StudentScoreView,
        meta: { roles: ['STUDENT'] }
      },
      {
        path: 'teacher/classes',
        name: 'teacher-classes',
        component: TeacherClassView,
        meta: { roles: ['TEACHER'] }
      },
      {
        path: 'teacher/scores',
        name: 'teacher-scores',
        component: TeacherScoreView,
        meta: { roles: ['TEACHER'] }
      },
      {
        path: 'admin/students',
        name: 'admin-students',
        component: AdminStudentView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/teachers',
        name: 'admin-teachers',
        component: AdminTeacherView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/courses',
        name: 'admin-courses',
        component: AdminCourseView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/course-selection-requests',
        name: 'admin-course-selection-requests',
        component: AdminSelectionRequestView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/statistics',
        name: 'admin-statistics',
        component: AdminStatisticsView,
        meta: { roles: ['ADMIN'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.public) {
    if (userStore.isLoggedIn && to.path === '/login') {
      return '/'
    }
    return true
  }
  if (!userStore.isLoggedIn) {
    return '/login'
  }
  const allowedRoles = to.meta.roles
  if (allowedRoles?.length && !allowedRoles.includes(userStore.roleCode)) {
    return '/'
  }
  return true
})

export default router
