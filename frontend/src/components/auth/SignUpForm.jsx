import * as styles from "../../css/LoginForm.css";

import { useState } from 'react';
import Button from '../common/Button.jsx'
import Card from '../common/Card.jsx'
import { authApi, getErrorMessage } from '../../api/client.js'

export default function SignUpForm({setNotice}) {

    const [signUpForm, setSignUpForm] = useState(
        {
            email : "",
            nickname : "",
            password : "",
        }
    )

    function validateSignUpForm() {
        if (!signUpForm.email.trim()) {
            setNotice('이메일을 입력해 주세요.')
            return false
        }

        if (!signUpForm.nickname.trim()) {
            setNotice('닉네임을 입력해 주세요.')
            return false
        }

        if (!signUpForm.password.trim()) {
            setNotice('비밀번호를 입력해 주세요.')
            return false
        }

        if (signUpForm.password.length < 6) {
            setNotice('비밀번호는 6자 이상 입력해 주세요.')
            return false
        }

        return true
    }

    async function handleSignUpSubmit(event) {
        event.preventDefault()

        if (!validateSignUpForm()) {
            return
        }

        const payload = {
            email: signUpForm.email.trim(),
            nickname: signUpForm.nickname.trim(),
            password: signUpForm.password
        }

        try {
            console.log('회원가입 payload:', payload)
            await authApi.signup(payload)

            setNotice('회원가입이 완료되었습니다. 로그인해 주세요.')
            setSignUpForm({ email: '', nickname: '', password: '' })
        } catch (error) {
            setNotice(getErrorMessage(error))
            console.error(error)
        }
    }

    return (
        <Card as="form" onSubmit={handleSignUpSubmit} className={styles.form} >
            <h1>회원가입 폼</h1>
            <label className={styles.field}>
                이메일
                <input className={styles.input}
                       type="email"
                       placeholder="email"
                       value={signUpForm.email}
                       onChange={(event) =>
                           setSignUpForm({
                                ...signUpForm,
                                email: event.target.value,


                    })
                }
                />
            </label>
            <label className={styles.field}>
                닉네임
                <input className={styles.input}
                       type="text" placeholder="text"
                       value={signUpForm.nickname}
                       onChange={(event) =>
                           setSignUpForm({
                                ...signUpForm,
                                nickname: event.target.value,
                            })
                }
                />
            </label>
            <label className={styles.field}>
                비밀번호
                <input className={styles.input}
                       type="password"
                       placeholder="password"
                       value={signUpForm.password}
                       onChange={(event) =>
                           setSignUpForm({
                               ...signUpForm,
                               password: event.target.value
                           })
                }
                />
            </label>
            <Button size="lg" type="submit">회원가입</Button>
        </Card>
    )
}
