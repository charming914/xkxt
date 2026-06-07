import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import LoginView from '../views/LoginView.vue'
import LayoutView from '../layouts/LayoutView.vue'
import DashboardView from '../views/DashboardView.vue'
import StudentProfileView from '../views/student/StudentProfileView.vue'
import StudentCourseView from '../views/student/StudentCourseView.vue'
import StudentCoursePlanView from '../views/student/StudentCoursePlanView.vue'
import StudentSelectedCourseView from '../views/student/StudentSelectedCourseView.vue'
import StudentDropCourseView from '../views/student/StudentDropCourseView.vue'
import StudentTimetableView from '../views/student/StudentTimetableView.vue'
import StudentScoreView from '../views/student/StudentScoreView.vue'
import StudentExamView from '../views/student/StudentExamView.vue'
import StudentRemediationView from '../views/student/StudentRemediationView.vue'
import StudentAcademicProgressView from '../views/student/StudentAcademicProgressView.vue'
import TeacherClassView from '../views/teacher/TeacherClassView.vue'
import TeacherScoreView from '../views/teacher/TeacherScoreView.vue'
import TeacherDropRequestView from '../views/teacher/TeacherDropRequestView.vue'
import TeacherProfileView from '../views/teacher/TeacherProfileView.vue'
import TeacherScheduleRequestView from '../views/teacher/TeacherScheduleRequestView.vue'
import TeacherExamView from '../views/teacher/TeacherExamView.vue'
import AdminStudentView from '../views/admin/AdminStudentView.vue'
import AdminTeacherView from '../views/admin/AdminTeacherView.vue'
import AdminCourseView from '../views/admin/AdminCourseView.vue'
import AdminSemesterView from '../views/admin/AdminSemesterView.vue'
import AdminTeachingClassView from '../views/admin/AdminTeachingClassView.vue'
import AdminCultivationPlanView from '../views/admin/AdminCultivationPlanView.vue'
import AdminStatisticsView from '../views/admin/AdminStatisticsView.vue'
import AdminScoreAnalysisView from '../views/admin/AdminScoreAnalysisView.vue'
import AdminOperationLogView from '../views/admin/AdminOperationLogView.vue'
import AdminSelectionRequestView from '../views/admin/AdminSelectionRequestView.vue'
import AdminDropRequestView from '../views/admin/AdminDropRequestView.vue'
import AdminScheduleRequestView from '../views/admin/AdminScheduleRequestView.vue'
import AdminScoreChangeRequestView from '../views/admin/AdminScoreChangeRequestView.vue'
import AdminScoreReviewRequestView from '../views/admin/AdminScoreReviewRequestView.vue'
import AdminExamView from '../views/admin/AdminExamView.vue'
import AdminExamChangeRequestView from '../views/admin/AdminExamChangeRequestView.vue'
import AdminRemediationRequestView from '../views/admin/AdminRemediationRequestView.vue'
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
        path: 'student/profile',
        name: 'student-profile',
        component: StudentProfileView,
        meta: { roles: ['STUDENT'] }
      },
      {
        path: 'student/courses',
        name: 'student-courses',
        component: StudentCourseView,
        meta: { roles: ['STUDENT'] }
      },
      {
        path: 'student/course-plans',
        name: 'student-course-plans',
        component: StudentCoursePlanView,
        meta: { roles: ['STUDENT'] }
      },
      {
        path: 'student/selected-courses',
        name: 'student-selected-courses',
        component: StudentSelectedCourseView,
        meta: { roles: ['STUDENT'] }
      },
      {
        path: 'student/drop-courses',
        name: 'student-drop-courses',
        component: StudentDropCourseView,
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
        path: 'student/exams',
        name: 'student-exams',
        component: StudentExamView,
        meta: { roles: ['STUDENT'] }
      },
      {
        path: 'student/remediation',
        name: 'student-remediation',
        component: StudentRemediationView,
        meta: { roles: ['STUDENT'] }
      },
      {
        path: 'student/academic-progress',
        name: 'student-academic-progress',
        component: StudentAcademicProgressView,
        meta: { roles: ['STUDENT'] }
      },
      {
        path: 'teacher/profile',
        name: 'teacher-profile',
        component: TeacherProfileView,
        meta: { roles: ['TEACHER'] }
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
        path: 'teacher/course-drop-requests',
        name: 'teacher-course-drop-requests',
        component: TeacherDropRequestView,
        meta: { roles: ['TEACHER'] }
      },
      {
        path: 'teacher/schedule-adjust-requests',
        name: 'teacher-schedule-adjust-requests',
        component: TeacherScheduleRequestView,
        meta: { roles: ['TEACHER'] }
      },
      {
        path: 'teacher/exams',
        name: 'teacher-exams',
        component: TeacherExamView,
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
        path: 'admin/semesters',
        name: 'admin-semesters',
        component: AdminSemesterView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/teaching-classes',
        name: 'admin-teaching-classes',
        component: AdminTeachingClassView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/exams',
        name: 'admin-exams',
        component: AdminExamView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/cultivation-plan',
        name: 'admin-cultivation-plan',
        component: AdminCultivationPlanView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/course-selection-requests',
        name: 'admin-course-selection-requests',
        component: AdminSelectionRequestView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/course-drop-requests',
        name: 'admin-course-drop-requests',
        component: AdminDropRequestView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/schedule-adjust-requests',
        name: 'admin-schedule-adjust-requests',
        component: AdminScheduleRequestView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/exam-change-requests',
        name: 'admin-exam-change-requests',
        component: AdminExamChangeRequestView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/score-change-requests',
        name: 'admin-score-change-requests',
        component: AdminScoreChangeRequestView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/score-review-requests',
        name: 'admin-score-review-requests',
        component: AdminScoreReviewRequestView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/remediation-requests',
        name: 'admin-remediation-requests',
        component: AdminRemediationRequestView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/statistics',
        name: 'admin-statistics',
        component: AdminStatisticsView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/score-analysis',
        name: 'admin-score-analysis',
        component: AdminScoreAnalysisView,
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'admin/operation-logs',
        name: 'admin-operation-logs',
        component: AdminOperationLogView,
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
