import {execSync} from 'child_process';
import {GoogleGenAI} from '@google/genai';

const gemini = new GoogleGenAI({apiKey: process.env.GEMINI_API_KEY});

const baseBranch = process.argv[2] || 'main';

async function runReview() {
    try {
        console.log(`[${baseBranch}] 브랜치 기준으로 변경 사항을 추출합니다...`);

        // 1. Base 브랜치와 현재 작업 브랜치의 공통 조상(Merge Base) 찾기
        const mergeBase = execSync(`git merge-base ${baseBranch} HEAD`, {encoding: 'utf8'}).trim();

        // 2. 공통 조상 시점부터 현재 워킹 트리(Uncommitted 포함)까지의 모든 변경 사항 추출
        const diff = execSync(`git diff ${mergeBase}`, {encoding: 'utf8'});

        if (!diff || diff.trim() === '') {
            console.log('✅ 리뷰할 변경 사항이 없습니다.');
            return;
        }

        const prompt = `
        당신은 Spring Boot 기반 커머스 서버의 수석 아키텍트입니다.
        아래 제공된 Git Diff 코드를 다음 관점에서 분석하고 심층 리뷰를 제공해주세요.
        
        [핵심 리뷰 포인트]
        1. 보안 취약점
        2. 성능 이슈
        3. 코드 스타일 및 아키텍처 위반
        
        단순한 오타나 포맷팅 지적은 생략하고, 치명적인 잠재적 장애 포인트만 짚어주세요.

        [Git Diff 코드] 
        ${diff}
        `;

        console.log('✨ Gemini 모델이 코드를 심층 분석 중입니다...\n');

        const response = await gemini.models.generateContent({
            model: 'gemini-2.5-flash',
            contents: prompt,
        });

        console.log('==================================================');
        console.log('🛠️ [Gemini Code Review Report]');
        console.log('==================================================\n');
        console.log(response.text);
    } catch (error) {
        console.error('❌ 리뷰 과정 중 오류 발생:', error.message);
        console.error('안내: 로컬에 변경 사항이 없거나, 지정한 base branch가 존재하는지 확인하세요.');
    }
}

runReview();