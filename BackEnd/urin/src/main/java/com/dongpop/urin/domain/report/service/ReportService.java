package com.dongpop.urin.domain.report.service;

import com.dongpop.urin.domain.analysis.dto.request.AnalysisCacheDto;
import com.dongpop.urin.domain.analysis.entity.*;
import com.dongpop.urin.domain.analysis.repository.AnalysisRepository;
import com.dongpop.urin.domain.analysis.repository.EmotionRepository;
import com.dongpop.urin.domain.analysis.repository.PostureRepository;
import com.dongpop.urin.domain.analysis.service.AnalysisService;
import com.dongpop.urin.domain.feedback.entity.Feedback;
import com.dongpop.urin.domain.feedback.repository.FeedbackRepository;
import com.dongpop.urin.domain.feedbackcontent.entity.FeedbackContent;
import com.dongpop.urin.domain.feedbackcontent.entity.FeedbackContentType;
import com.dongpop.urin.domain.meeting.entity.Meeting;
import com.dongpop.urin.domain.meeting.repository.MeetingRepository;
import com.dongpop.urin.domain.meetingParticipant.repository.MeetingParticipantRepository;
import com.dongpop.urin.domain.member.entity.Member;
import com.dongpop.urin.domain.member.repository.MemberRepository;
import com.dongpop.urin.domain.report.dto.response.ReportDataDto;
import com.dongpop.urin.domain.report.dto.response.analysis.AIDataDto;
import com.dongpop.urin.domain.report.dto.response.analysis.ComparisonDataDto;
import com.dongpop.urin.domain.report.dto.response.analysis.EmotionDataDto;
import com.dongpop.urin.domain.report.dto.response.analysis.PoseDataDto;
import com.dongpop.urin.domain.report.dto.response.feedback.AnswerContentDto;
import com.dongpop.urin.domain.report.dto.response.feedback.FeedbackDataDto;
import com.dongpop.urin.domain.report.dto.response.feedback.QuestionContentDto;
import com.dongpop.urin.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dongpop.urin.global.error.errorcode.CommonErrorCode.DO_NOT_HAVE_AUTHORIZATION;
import static com.dongpop.urin.global.error.errorcode.CommonErrorCode.NO_SUCH_ELEMENTS;
import static com.dongpop.urin.global.error.errorcode.MeetingErrorCode.MEETING_IS_NOT_EXIST;

@RequiredArgsConstructor
@Service
public class ReportService {

    private final AnalysisRepository analysisRepository;
    private final EmotionRepository emotionRepository;
    private final PostureRepository postureRepository;
    private final MeetingRepository meetingRepository;
    private final MemberRepository memberRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final FeedbackRepository feedbackRepository;
    private final AnalysisService analysisService;

    public ReportDataDto setReportData(int meetingId, int intervieweeId) {
        Meeting meeting = getMeeting(meetingId);
        Member interviewee = getMember(intervieweeId);

        checkMemberExistence(meeting, interviewee);

        AnalysisData analysisData = getAnalysisData(meeting, interviewee);

        return ReportDataDto.builder()
                .aiData(ComparisonDataDto.builder()
                        .interviewee(getAIReportData(analysisData))
                        .passUser(getpassedAIData())
                        .build())
                .feedback(getFeedbackData(feedbackRepository.findByMeetingAndInterviewee(meeting, interviewee))).build();
    }

    private FeedbackDataDto getFeedbackData(List<Feedback> feedbackList) {

        List<QuestionContentDto> tech = new ArrayList<>();
        List<QuestionContentDto> personality = new ArrayList<>();

        for(Feedback feedback : feedbackList) {
            //여기서 피드백의 Question 넣어주기


            //feedback content iter


            //answer리스트 만들기


            //answer 넣어주기

            List<FeedbackContent> feedbackContents = feedback.getFeedbackContents();

            List<AnswerContentDto> techAnswer = new ArrayList<>();
            List<AnswerContentDto> perAnswer = new ArrayList<>();

            for (FeedbackContent feedbackContent : feedbackContents) {
                AnswerContentDto answerContentDto = AnswerContentDto.builder()
                        .content(feedbackContent.getAnswer())
                        .interviewer(feedback.getInterviewer().getMemberName()).build();

                if (FeedbackContentType.TECH.equals(feedbackContent.getType())) {
                    techAnswer.add(answerContentDto);
                } else {
                    perAnswer.add(answerContentDto);
                }
            }

            tech.add(QuestionContentDto.builder()
                    .questionContent(feedbackContents.get(0).getQuestion())
                    .answerContentList(techAnswer)
                    .build());
        }

        return FeedbackDataDto.builder()
                .tech(tech)
                .personality(personality).build();
    }

    private AIDataDto getpassedAIData() {
        Map<String, AnalysisCacheDto> passedData = analysisService.getPassDataCache();

        List<PoseDataDto> poseDataDtoList = new ArrayList<>();
        for(PostureType type : PostureType.values()) {
            poseDataDtoList.add(PoseDataDto.builder()
                    .name(type.name())
                    .count(passedData.get(type.name()).getData()).build());
        }

        return AIDataDto.builder()
                .emotion(
                        EmotionDataDto.builder()
                        .confidence((int) Math.floor(passedData.get(EmotionType.CONFIDENCE.name()).getData()))
                        .calm((int) Math.floor(passedData.get(EmotionType.CALM.name()).getData()))
                        .nervous((int) Math.floor(passedData.get(EmotionType.CONFIDENCE.name()).getData())).build())
                .poseDataList(poseDataDtoList)
                .build();
    }

    private AIDataDto getAIReportData(AnalysisData analysisData) {
        double time = Double.valueOf(analysisData.getTime());

        return AIDataDto.builder()
                .emotion(calcIntervieweeEmotion(emotionRepository.findByAnalysisData(analysisData), time))
                .poseDataList(calcIntervieweePose(postureRepository.findByAnalysisData(analysisData), time))
                .build();
    }

    private EmotionDataDto calcIntervieweeEmotion(List<Emotion> emotionList, double time) {

        Map<EmotionType, Double> emotionMap = emotionList.stream()
                .collect(Collectors.toMap(Emotion::getType, v -> Double.valueOf(v.getCount())));

        return EmotionDataDto.builder()
                .confidence((int) Math.floor((emotionMap.get(EmotionType.CONFIDENCE) / time) * 100))
                .calm((int) Math.floor((emotionMap.get(EmotionType.CALM) / time) * 100))
                .nervous((int) Math.floor((emotionMap.get(EmotionType.NERVOUS) / time) * 100)).build();
    }

    private List<PoseDataDto> calcIntervieweePose(List<Posture> postureList, double time) {
        List<PoseDataDto> poseDataDtoList = new ArrayList<>();

        for(Posture posture : postureList) {
            poseDataDtoList.add(PoseDataDto.builder()
                    .name(posture.getType().getPostureKorean())
                    .count(Math.round((Double.valueOf(posture.getCount()) / time) * 60 * 10) / 10).build());
        }

        return poseDataDtoList;
    }

    private void checkMemberExistence(Meeting meeting, Member interviewee) {
        if(meetingParticipantRepository.findByMeetingAndMember(meeting, interviewee).isEmpty())
            throw new CustomException(DO_NOT_HAVE_AUTHORIZATION);
    }

    private AnalysisData getAnalysisData(Meeting meeting, Member interviewee) {
        return analysisRepository.findByMeetingAndInterviewee(meeting, interviewee)
                .orElseThrow(() -> new CustomException(NO_SUCH_ELEMENTS));
    }

    private Member getMember(int memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(NO_SUCH_ELEMENTS));
    }

    private Meeting getMeeting(int meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new CustomException(MEETING_IS_NOT_EXIST));
    }
}
